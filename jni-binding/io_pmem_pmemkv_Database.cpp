// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

#include <cstring>
#include <string>
#include <jni.h>
#include <libpmemkv.hpp>
#include <unordered_map>
#include <iostream>
#include <memory>
#include <utility>

#define DO_LOG 0
#define LOG(msg) if (DO_LOG) std::cout << "[pmemkv-jni] " << msg << "\n"

class PmemkvJavaException {
private:
    static std::unordered_map<pmem::kv::status, const char*> PmemkvStatusDispatcher;
    JNIEnv* env;

public:
    constexpr static const char* DatabaseException = "io/pmem/pmemkv/DatabaseException";
    constexpr static const char* GeneralException = "java/lang/Error";

    PmemkvJavaException(JNIEnv* env_) {
        env = env_;
    }

    void ThrowException(pmem::kv::status status, const char* msg =  pmemkv_errormsg()){
        jclass exception_class;
        exception_class = env->FindClass(PmemkvStatusDispatcher[status]);
        if (exception_class == NULL) {
            exception_class = env->FindClass(DatabaseException);
        }
        if (exception_class == NULL) {
            exception_class = env->FindClass(GeneralException);
        }
        env->ThrowNew(exception_class, msg);
    }

    void ThrowException(const char* signature, const char* msg=""){
        jclass exception_class;
        exception_class = env->FindClass(signature);
        if (exception_class == NULL) {
            exception_class = env->FindClass(GeneralException);
        }
        env->ThrowNew(exception_class, msg);
        env->DeleteLocalRef(exception_class);
    }
};

std::unordered_map<pmem::kv::status, const char*> PmemkvJavaException::PmemkvStatusDispatcher = {
       { pmem::kv::status::UNKNOWN_ERROR, "io/pmem/pmemkv/DatabaseException" },
       { pmem::kv::status::NOT_FOUND, "io/pmem/pmemkv/NotFoundException"},
       { pmem::kv::status::NOT_SUPPORTED, "io/pmem/pmemkv/NotSupportedException"},
       { pmem::kv::status::INVALID_ARGUMENT, "io/pmem/pmemkv/InvalidArgumentException"},
       { pmem::kv::status::CONFIG_PARSING_ERROR, "io/pmem/pmemkv/BuilderException"},
       { pmem::kv::status::CONFIG_TYPE_ERROR, "io/pmem/pmemkv/BuilderException"},
       { pmem::kv::status::STOPPED_BY_CB, "io/pmem/pmemkv/StoppedByCallbackException"},
       { pmem::kv::status::OUT_OF_MEMORY, "io/pmem/pmemkv/OutOfMemoryException"},
       { pmem::kv::status::WRONG_ENGINE_NAME, "io/pmem/pmemkv/WrongEngineNameException"},
       { pmem::kv::status::TRANSACTION_SCOPE_ERROR, "io/pmem/pmemkv/TransactionScopeException"},
};

#define KEY_CALLBACK_NAME "keyCallbackWrapper"
#define VALUE_CALLBACK_NAME "valueCallbackWrapper"
#define KEY_VALUE_CALLBACK_NAME "keyValueCallbackWrapper"
#define KEY_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyCallback;ILjava/nio/ByteBuffer;)V"
#define VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/ValueCallback;ILjava/nio/ByteBuffer;)V"
#define KEY_VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyValueCallback;ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"

jmethodID keyCallbackID = NULL;
jmethodID valueCallbackID = NULL;
jmethodID keyValueCallbackID = NULL;

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1start
        (JNIEnv* env, jobject obj, jstring engine, jlong config) {
    const char* cengine = env->GetStringUTFChars(engine, NULL);

    keyCallbackID = env->GetStaticMethodID(env->GetObjectClass(obj), KEY_CALLBACK_NAME, KEY_CALLBACK_SIG);
    valueCallbackID = env->GetStaticMethodID(env->GetObjectClass(obj), VALUE_CALLBACK_NAME, VALUE_CALLBACK_SIG);
    keyValueCallbackID = env->GetStaticMethodID(env->GetObjectClass(obj), KEY_VALUE_CALLBACK_NAME, KEY_VALUE_CALLBACK_SIG);

    pmem::kv::db *db = new pmem::kv::db();
    auto cfg = reinterpret_cast<pmemkv_config*>(config);
    pmem::kv::status status = db->open(cengine, pmem::kv::config(cfg));
    env->ReleaseStringUTFChars(engine, cengine);

    if (status != pmem::kv::status::OK) {
        PmemkvJavaException(env).ThrowException(status);
    }
    return (jlong) db;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1stop
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    engine->close();
}

struct Context {
    JNIEnv* env;
    jobject db;
    jobject callback;
    jmethodID mid;

    Context(JNIEnv* env_, jobject db_, jobject callback_, jmethodID mid_) {
        env = env_;
        db = db_;
        callback = callback_;
        mid = mid_;
    }
};

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
    if (c->env->ExceptionCheck() == JNI_TRUE) {
        return 1;
    }
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    auto ctx = Context(env, obj, callback, keyCallbackID);
    auto status = engine->get_all(Callback_get_keys_buffer, &ctx);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto cxt = Context(env, obj, callback, keyCallbackID);
    auto status = engine->get_above(cppkey, Callback_get_keys_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto cxt = Context(env, obj, callback, keyCallbackID);
    auto status = engine->get_below(cppkey, Callback_get_keys_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    pmem::kv::string_view cppkey1(ckey1, keybytes1);
    pmem::kv::string_view cppkey2(ckey2, keybytes2);
    auto cxt = Context(env, obj, callback, keyCallbackID);
    auto status = engine->get_between(cppkey1, cppkey2, Callback_get_keys_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1all
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    size_t count;
    auto status = engine->count_all(count);
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    size_t count;
    auto status = engine->count_above(cppkey, count);
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    size_t count;
    auto status = engine->count_below(cppkey, count);
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    pmem::kv::string_view cppkey1(ckey1, keybytes1);
    pmem::kv::string_view cppkey2(ckey2, keybytes2);
    size_t count;
    auto status = engine->count_between(cppkey1, cppkey2, count);
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);

    return count;
}

int Callback_get_all_buffer(const char* k, size_t kb, const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);

    jobject keybuf = c->env->NewDirectByteBuffer(const_cast<char*>(k), kb);
    if (c->env->ExceptionCheck() == JNI_TRUE)
        return 1;
    jobject valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb);
    if (c->env->ExceptionCheck() == JNI_TRUE)
        return 1;

    if(keybuf && valuebuf) {
        c->env->CallStaticVoidMethod(c->env->GetObjectClass(c->db), c->mid, c->db, c->callback, kb, keybuf, vb, valuebuf);
        c->env->DeleteLocalRef(keybuf);
        c->env->DeleteLocalRef(valuebuf);
    }
    if (c->env->ExceptionCheck() == JNI_TRUE) {
        return 1;
    }
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1all_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    auto cxt = Context(env, obj, callback, keyValueCallbackID);
    auto status = engine->get_all(Callback_get_all_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE)
        return; // Propagate exception raised by CallStaticVoidMethod()
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto cxt = Context(env, obj, callback, keyValueCallbackID);
    auto status = engine->get_above(cppkey, Callback_get_all_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE)
        return; // Propagate exception raised by CallStaticVoidMethod()
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto cxt = Context(env, obj, callback, keyValueCallbackID);
    auto status = engine->get_below(cppkey, Callback_get_all_buffer,&cxt);
    if (env->ExceptionCheck() == JNI_TRUE)
        return; // Propagate exception raised by CallStaticVoidMethod()
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    pmem::kv::string_view cppkey1(ckey1, keybytes1);
    pmem::kv::string_view cppkey2(ckey2, keybytes2);
    auto cxt = Context(env, obj, callback, keyValueCallbackID);
    auto status = engine->get_between(cppkey1, cppkey2, Callback_get_all_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE)
        return; // Propagate exception raised by CallStaticVoidMethod()
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1exists_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto status = engine->exists(cppkey);
    if (status != pmem::kv::status::OK && status != pmem::kv::status::NOT_FOUND)
        PmemkvJavaException(env).ThrowException(status);
    return status == pmem::kv::status::OK;
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
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    auto cxt = Context(env, obj, callback, valueCallbackID);
    auto status = engine->get(cppkey, Callback_get_value_buffer, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE) return;
    if (status != pmem::kv::status::OK) PmemkvJavaException(env).ThrowException(status);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_Database_database_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes ,jobject key) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    ContextGetByteArray cxt = ContextGetByteArray(env);
    auto status = engine->get(cppkey, callback_get_byte_array, &cxt);
    if (env->ExceptionCheck() == JNI_TRUE) return nullptr;
    if (status != pmem::kv::status::OK)
        PmemkvJavaException(env).ThrowException(status);
    return cxt.result;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1put_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const char* cvalue = (char*) env->GetDirectBufferAddress(value);
    pmem::kv::string_view cppkey(ckey, keybytes);
    pmem::kv::string_view cppvalue(cvalue, valuebytes);
    const auto result = engine->put(cppkey, cppvalue);
    if (result != pmem::kv::status::OK)
        PmemkvJavaException(env).ThrowException(result);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = reinterpret_cast<pmem::kv::db*>(pointer);
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    pmem::kv::string_view cppkey(ckey, keybytes);
    const auto result = engine->remove(cppkey);
    if (result != pmem::kv::status::OK  && result != pmem::kv::status::NOT_FOUND)
         PmemkvJavaException(env).ThrowException(result);
    return result == pmem::kv::status::OK;
}
