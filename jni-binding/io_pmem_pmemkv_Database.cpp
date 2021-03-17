// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

#include <cstring>
#include <string>
#include <jni.h>
#include <libpmemkv.h>
#include <unordered_map>
#include <iostream>

#define DO_LOG 0
#define LOG(msg) if (DO_LOG) std::cout << "[pmemkv-jni] " << msg << "\n"

class PmemkvJavaException {
private:
    static std::unordered_map<int, const char*> PmemkvStatusDispatcher;
    JNIEnv* env;

public:
    constexpr static const char* DatabaseException = "io/pmem/pmemkv/DatabaseException";
    constexpr static const char* GeneralException = "java/lang/Error";

    PmemkvJavaException(JNIEnv* env_) {
        env = env_;
    }

    void ThrowException(int status, const char* msg =  pmemkv_errormsg()){
        jclass exception_class;
        exception_class = env->FindClass(PmemkvStatusDispatcher[status]);
        if(exception_class == NULL) {
            exception_class = env->FindClass(DatabaseException);
        }
        if(exception_class == NULL) {
            exception_class = env->FindClass(GeneralException);
        }
        env->ThrowNew(exception_class, msg);
    }

    void ThrowException(const char* signature, const char* msg=""){
        jclass exception_class;
        exception_class = env->FindClass(signature);
        if(exception_class == NULL) {
            exception_class = env->FindClass(GeneralException);
        }
        env->ThrowNew(exception_class, msg);
    }
};

std::unordered_map<int, const char*> PmemkvJavaException::PmemkvStatusDispatcher = {
       { PMEMKV_STATUS_UNKNOWN_ERROR, "io/pmem/pmemkv/DatabaseException" },
       { PMEMKV_STATUS_NOT_FOUND, "io/pmem/pmemkv/NotFoundException"},
       { PMEMKV_STATUS_NOT_SUPPORTED, "io/pmem/pmemkv/NotSupportedException"},
       { PMEMKV_STATUS_INVALID_ARGUMENT, "io/pmem/pmemkv/InvalidArgumentException"},
       { PMEMKV_STATUS_CONFIG_PARSING_ERROR, "io/pmem/pmemkv/BuilderException"},
       { PMEMKV_STATUS_CONFIG_TYPE_ERROR, "io/pmem/pmemkv/BuilderException"},
       { PMEMKV_STATUS_STOPPED_BY_CB, "io/pmem/pmemkv/StoppedByCallbackException"},
       { PMEMKV_STATUS_OUT_OF_MEMORY, "io/pmem/pmemkv/OutOfMemoryException"},
       { PMEMKV_STATUS_WRONG_ENGINE_NAME, "io/pmem/pmemkv/WrongEngineNameException"},
       { PMEMKV_STATUS_TRANSACTION_SCOPE_ERROR, "io/pmem/pmemkv/TransactionScopeException"},
};

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1start
        (JNIEnv* env, jobject obj, jstring engine, jlong config) {
    const char* cengine = env->GetStringUTFChars(engine, NULL);

    pmemkv_db *db;
    auto status = pmemkv_open(cengine, (pmemkv_config*) config, &db);
    env->ReleaseStringUTFChars(engine, cengine);

    if (status != PMEMKV_STATUS_OK) {
            PmemkvJavaException(env).ThrowException(status);
    }
    return (jlong) db;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1stop
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = (pmemkv_db*) pointer;
    pmemkv_close(engine);
}

jmethodID keyCallbackID = NULL;
jmethodID valueCallbackID = NULL;
jmethodID keyValueCallbackID = NULL;

struct Context {
    JNIEnv* env;
    jobject db;
    jobject callback;
    jmethodID mid;

    Context(JNIEnv* env_, jobject db_, jobject callback_, jmethodID &mid_, const char *wrapperName, const char *wrapperSignature) {
        env = env_;
        db = db_;
        callback = callback_;

        if (!mid_) {
            mid_ = env->GetStaticMethodID(env->GetObjectClass(db), wrapperName, wrapperSignature);
        }
        mid = mid_;
    }
};

#define KEY_CALLBACK_NAME "keyCallbackWrapper"
#define VALUE_CALLBACK_NAME "valueCallbackWrapper"
#define KEY_VALUE_CALLBACK_NAME "keyValueCallbackWrapper"
#define KEY_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyCallback;ILjava/nio/ByteBuffer;)V"
#define VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/ValueCallback;ILjava/nio/ByteBuffer;)V"
#define KEY_VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyValueCallback;ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"

void Callback_get_value_buffer(const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);
    // OutOfMemoryError may occurr
    if (jobject valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb)) {
        // Rerise exception
        c->env->CallStaticVoidMethod(c->env->GetObjectClass(c->db), c->mid, c->db, c->callback, vb, valuebuf);
        c->env->DeleteLocalRef(valuebuf);
    }
}

int Callback_get_keys_buffer(const char* k, size_t kb, const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);
    Callback_get_value_buffer(k, kb, arg);
    if (c->env->ExceptionOccurred()) {
        return 1;
    }
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    auto cxt = Context(env, obj, callback, keyCallbackID, KEY_CALLBACK_NAME, KEY_CALLBACK_SIG);
    auto status = pmemkv_get_all(engine, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, obj, callback, keyCallbackID, KEY_CALLBACK_NAME, KEY_CALLBACK_SIG);
    auto status = pmemkv_get_above(engine, ckey, keybytes, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, obj, callback, keyCallbackID, KEY_CALLBACK_NAME, KEY_CALLBACK_SIG);
    auto status = pmemkv_get_below(engine, ckey, keybytes, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    auto cxt = Context(env, obj, callback, keyCallbackID, KEY_CALLBACK_NAME, KEY_CALLBACK_SIG);
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1all
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = (pmemkv_db*) pointer;
    size_t count;
    auto status = pmemkv_count_all(engine, &count);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);

    size_t count;
    auto status = pmemkv_count_above(engine, ckey, keybytes, &count);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);

    size_t count;
    auto status = pmemkv_count_below(engine, ckey, keybytes, &count);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);

    size_t count;
    auto status = pmemkv_count_between(engine, ckey1, keybytes1, ckey2, keybytes2, &count);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

int Callback_get_all_buffer(const char* k, size_t kb, const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);

    jobject keybuf = c->env->NewDirectByteBuffer(const_cast<char*>(k), kb);
    jobject valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb);
    if(keybuf && valuebuf) {
        c->env->CallStaticVoidMethod(c->env->GetObjectClass(c->db), c->mid, c->db, c->callback, kb, keybuf, vb, valuebuf);
        c->env->DeleteLocalRef(keybuf);
        c->env->DeleteLocalRef(valuebuf);
    }
    if( c->env->ExceptionOccurred()) {
        return 1;
    }
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1all_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    auto cxt = Context(env, obj, callback, keyValueCallbackID, KEY_VALUE_CALLBACK_NAME, KEY_VALUE_CALLBACK_SIG);
    auto status = pmemkv_get_all(engine, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, obj, callback, keyValueCallbackID, KEY_VALUE_CALLBACK_NAME, KEY_VALUE_CALLBACK_SIG);
    auto status = pmemkv_get_above(engine, ckey, keybytes, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, obj, callback, keyValueCallbackID, KEY_VALUE_CALLBACK_NAME, KEY_VALUE_CALLBACK_SIG);
    auto status = pmemkv_get_below(engine, ckey, keybytes, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    auto cxt = Context(env, obj, callback, keyValueCallbackID, KEY_VALUE_CALLBACK_NAME, KEY_VALUE_CALLBACK_SIG);
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1exists_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto status = pmemkv_exists(engine, ckey, keybytes);
    if (status != PMEMKV_STATUS_OK && status != PMEMKV_STATUS_NOT_FOUND)
        PmemkvJavaException(env).ThrowException(status);
    return status == PMEMKV_STATUS_OK;
}

struct ContextGetByteArray {
    JNIEnv* env;
    jbyteArray result;

    ContextGetByteArray(JNIEnv* env_, jbyteArray result_ = NULL){
        env = env_;
        result = result_;
    }
};

void callback_get_byte_array(const char* v, size_t vb, void *arg) {
    const auto c = ((ContextGetByteArray*) arg);
    if((c->result = c->env->NewByteArray(vb))) {
        c->env->SetByteArrayRegion(c->result, 0, vb, (jbyte*) v);
    } else {
        PmemkvJavaException ex = PmemkvJavaException(c->env);
        ex.ThrowException(PmemkvJavaException::DatabaseException, "Cannot allocate output buffer");
    }
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1buffer_1with_1callback
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, obj, callback, valueCallbackID, VALUE_CALLBACK_NAME, VALUE_CALLBACK_SIG);
    auto status = pmemkv_get(engine, ckey, keybytes, Callback_get_value_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_Database_database_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes ,jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    ContextGetByteArray cxt = ContextGetByteArray(env);
    auto status = pmemkv_get(engine, (char*) ckey, keybytes, callback_get_byte_array, &cxt);
    if (status != PMEMKV_STATUS_OK)
        PmemkvJavaException(env).ThrowException(status);
    return cxt.result;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1put_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const char* cvalue = (char*) env->GetDirectBufferAddress(value);
    const auto result = pmemkv_put(engine, ckey, keybytes, cvalue, valuebytes);
    if (result != PMEMKV_STATUS_OK)
        PmemkvJavaException(env).ThrowException(result);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto result = pmemkv_remove(engine, ckey, keybytes);
    if (result != PMEMKV_STATUS_OK  && result != PMEMKV_STATUS_NOT_FOUND)
         PmemkvJavaException(env).ThrowException(result);
    return result == PMEMKV_STATUS_OK;
}
