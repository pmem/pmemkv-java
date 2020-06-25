// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

#include <cstring>
#include <string>
#include <jni.h>
#include <libpmemkv.h>
#include <libpmemkv_json_config.h>
#include <iostream>

#define DO_LOG 0
#define LOG(msg) if (DO_LOG) std::cout << "[pmemkv-jni] " << msg << "\n"

#define EXCEPTION_CLASS "io/pmem/pmemkv/DatabaseException"

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1start
        (JNIEnv* env, jobject obj, jstring engine, jstring config) {
    const char* cengine = env->GetStringUTFChars(engine, NULL);
    const char* cconfig = env->GetStringUTFChars(config, NULL);

    auto cfg = pmemkv_config_new();
    if (config == nullptr) {
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
        return 0;
    }

    auto status = pmemkv_config_from_json(cfg, cconfig);
    if (status != PMEMKV_STATUS_OK) {
        pmemkv_config_delete(cfg);
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
        return 0;
    }

    pmemkv_db *db;
    status = pmemkv_open(cengine, cfg, &db);

    env->ReleaseStringUTFChars(engine, cengine);
    env->ReleaseStringUTFChars(config, cconfig);

    if (status != PMEMKV_STATUS_OK)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());

    return (jlong) db;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1stop
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = (pmemkv_db*) pointer;
    pmemkv_close(engine);
}

struct Context {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;

    Context(JNIEnv* env_, jobject callback_, const char *sig) {
        env = env_;
        callback = callback_;
        mid = env->GetMethodID(env->GetObjectClass(callback), "process", sig);
    }
};

#define METHOD_GET_KEYS_BUFFER "(ILjava/nio/ByteBuffer;)V"
#define METHOD_GET_ALL_BUFFER "(ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"

void Callback_get_value_buffer(const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);
    // OutOfMemoryError may Occurr
    if( jobject valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb)) {
        // Rerise exception
        c->env->CallVoidMethod(c->callback, c->mid, vb, valuebuf);
        c->env->DeleteLocalRef(valuebuf);
    }
}

int Callback_get_keys_buffer(const char* k, size_t kb, const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);
    Callback_get_value_buffer(k, kb, arg);
    if( c->env->ExceptionOccurred()) {
        return 1;
    }
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    auto cxt = Context(env, callback,  METHOD_GET_KEYS_BUFFER);
    auto status = pmemkv_get_all(engine, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, callback,  METHOD_GET_KEYS_BUFFER);
    auto status = pmemkv_get_above(engine, ckey, keybytes, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, callback,  METHOD_GET_KEYS_BUFFER);
    auto status = pmemkv_get_below(engine, ckey, keybytes, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    auto cxt = Context(env, callback,  METHOD_GET_KEYS_BUFFER);
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, Callback_get_keys_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1all
        (JNIEnv* env, jobject obj, jlong pointer) {
    auto engine = (pmemkv_db*) pointer;
    size_t count;
    pmemkv_count_all(engine, &count);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);

    size_t count;
    pmemkv_count_above(engine, ckey, keybytes, &count);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);

    size_t count;
    pmemkv_count_below(engine, ckey, keybytes, &count);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);

    size_t count;
    pmemkv_count_between(engine, ckey1, keybytes1, ckey2, keybytes2, &count);

    return count;
}

int Callback_get_all_buffer(const char* k, size_t kb, const char* v, size_t vb, void *arg) {
    const auto c = static_cast<Context*>(arg);

    jobject keybuf = c->env->NewDirectByteBuffer( const_cast<char*>(k), kb);
    jobject valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb);
    if( keybuf && valuebuf) {
        c->env->CallVoidMethod(c->callback, c->mid, kb, keybuf, vb, valuebuf);
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
    auto cxt = Context(env, callback, METHOD_GET_ALL_BUFFER);
    auto status = pmemkv_get_all(engine, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, callback, METHOD_GET_ALL_BUFFER);
    auto status = pmemkv_get_above(engine, ckey, keybytes, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, callback, METHOD_GET_ALL_BUFFER);
    auto status = pmemkv_get_below(engine, ckey, keybytes, Callback_get_all_buffer,&cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    auto cxt = Context(env, callback, METHOD_GET_ALL_BUFFER);
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, Callback_get_all_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1exists_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto status = pmemkv_exists(engine, ckey, keybytes);
    if (status != PMEMKV_STATUS_OK && status != PMEMKV_STATUS_NOT_FOUND)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
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

void callback_get_byte_array(const char* v, size_t vb, void *arg)  {
    const auto c = ((ContextGetByteArray*) arg);
    if(c->result = c->env->NewByteArray(vb)){
        c->env->SetByteArrayRegion(c->result, 0, vb, (jbyte*) v);
    } else {
        c->env->ThrowNew(c->env->FindClass(EXCEPTION_CLASS), "Cannot allocate output buffer");
    }
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1buffer_1with_1callback
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    auto cxt = Context(env, callback, METHOD_GET_KEYS_BUFFER);
    auto status = pmemkv_get(engine, ckey, keybytes, Callback_get_value_buffer, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_Database_database_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes ,jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    ContextGetByteArray cxt = ContextGetByteArray(env);
    auto status = pmemkv_get(engine, (char*) ckey, keybytes, callback_get_byte_array, &cxt);
    if (status != PMEMKV_STATUS_OK)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
    return cxt.result;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1put_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const char* cvalue = (char*) env->GetDirectBufferAddress(value);
    const auto result = pmemkv_put(engine, ckey, keybytes, cvalue, valuebytes);
    if (result != PMEMKV_STATUS_OK)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto result = pmemkv_remove(engine, ckey, keybytes);
    if (result != PMEMKV_STATUS_OK  && result != PMEMKV_STATUS_NOT_FOUND)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
    return result == PMEMKV_STATUS_OK;
}
