/*
 * Copyright 2017-2019, Intel Corporation
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *
 *     * Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
};

#define CONTEXT {env, callback, mid}

#define METHOD_GET_KEYS_BUFFER "(ILjava/nio/ByteBuffer;)V"
#define METHOD_GET_KEYS_BYTEARRAY "([B)V"
#define METHOD_GET_KEYS_STRING "(Ljava/lang/String;)V"
#define METHOD_GET_ALL_BUFFER "(ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"
#define METHOD_GET_ALL_BYTEARRAY "([B[B)V"
#define METHOD_GET_ALL_STRING "(Ljava/lang/String;Ljava/lang/String;)V"

struct ContextGetKeysBuffer {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
    int keybytes;
    char* key;
    jobject keybuf;
};

#define CONTEXT_GET_KEYS_BUFFER {env, callback, mid, 0, nullptr, nullptr}

const auto CALLBACK_GET_KEYS_BUFFER = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = static_cast<ContextGetKeysBuffer*>(arg);
    if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
    c->keybuf = c->env->NewDirectByteBuffer(const_cast<char*>(k), kb);
    c->env->CallVoidMethod(c->callback, c->mid, kb, c->keybuf);
    c->keybuf = nullptr;
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BUFFER);
    ContextGetKeysBuffer cxt = CONTEXT_GET_KEYS_BUFFER;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_KEYS_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BUFFER);
    ContextGetKeysBuffer cxt = CONTEXT_GET_KEYS_BUFFER;
    auto status = pmemkv_get_above(engine, ckey, keybytes, CALLBACK_GET_KEYS_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BUFFER);
    ContextGetKeysBuffer cxt = CONTEXT_GET_KEYS_BUFFER;
    auto status = pmemkv_get_below(engine, ckey, keybytes, CALLBACK_GET_KEYS_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BUFFER);
    ContextGetKeysBuffer cxt = CONTEXT_GET_KEYS_BUFFER;
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, CALLBACK_GET_KEYS_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

const auto CALLBACK_GET_KEYS_BYTEARRAY = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = ((Context*) arg);
    const auto ckey = c->env->NewByteArray(kb);
    c->env->SetByteArrayRegion(ckey, 0, kb, (jbyte*) k);
    c->env->CallVoidMethod(c->callback, c->mid, ckey);
    c->env->DeleteLocalRef(ckey);
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_KEYS_BYTEARRAY, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_above(engine, (char *) ckey, ckeybytes, CALLBACK_GET_KEYS_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_below(engine, (char*) ckey, ckeybytes, CALLBACK_GET_KEYS_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_between(engine, (char*) ckey1, ckeybytes1, (char*) ckey2, ckeybytes2, CALLBACK_GET_KEYS_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

const auto CALLBACK_GET_KEYS_STRING = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = ((Context*) arg);
    const auto ckey = c->env->NewStringUTF(k);
    c->env->CallVoidMethod(c->callback, c->mid, ckey);
    c->env->DeleteLocalRef(ckey);
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_KEYS_STRING, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1above_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_above(engine, (char*) ckey, ckeybytes, CALLBACK_GET_KEYS_STRING, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1below_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_below(engine, (char*) ckey, ckeybytes, CALLBACK_GET_KEYS_STRING, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1keys_1between_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_KEYS_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_between(engine, (char*) ckey1, ckeybytes1, (char*) ckey2, ckeybytes2, CALLBACK_GET_KEYS_STRING, &cxt);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
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

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
        
    size_t count;
    pmemkv_count_above(engine, (char *)ckey, ckeybytes, &count);

    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);

    size_t count;
    pmemkv_count_below(engine, (char*) ckey, ckeybytes, &count);

    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);

    return count;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_database_1count_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);

    size_t count;
    pmemkv_count_between(engine, (char*) ckey1, ckeybytes1, (char*) ckey2, ckeybytes2, &count);

    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
    return count;
}

struct ContextGetAllBuffer {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
    int keybytes;
    char* key;
    jobject keybuf;
    int valuebytes;
    char* value;
    jobject valuebuf;
};

#define CONTEXT_GET_ALL_BUFFER {env, callback, mid, 0, nullptr, nullptr, 0, nullptr, nullptr}

const auto CALLBACK_GET_ALL_BUFFER = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = static_cast<ContextGetAllBuffer*>(arg);
    if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
    c->keybuf = c->env->NewDirectByteBuffer( const_cast<char*>(k), kb);

    if (c->valuebuf != nullptr) c->env->DeleteLocalRef(c->valuebuf);
    c->valuebuf = c->env->NewDirectByteBuffer(const_cast<char*>(v), vb);

    c->env->CallVoidMethod(c->callback, c->mid, kb, c->keybuf, vb, c->valuebuf);

    c->keybuf = nullptr;
    c->valuebuf = nullptr;
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1all_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BUFFER);
    ContextGetAllBuffer cxt = CONTEXT_GET_ALL_BUFFER;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_ALL_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BUFFER);
    ContextGetAllBuffer cxt = CONTEXT_GET_ALL_BUFFER;
    auto status = pmemkv_get_above(engine, ckey, keybytes, CALLBACK_GET_ALL_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BUFFER);
    ContextGetAllBuffer cxt = CONTEXT_GET_ALL_BUFFER;
    auto status = pmemkv_get_below(engine, ckey, keybytes, CALLBACK_GET_ALL_BUFFER,&cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BUFFER);
    ContextGetAllBuffer cxt = CONTEXT_GET_ALL_BUFFER;
    auto status = pmemkv_get_between(engine, ckey1, keybytes1, ckey2, keybytes2, CALLBACK_GET_ALL_BUFFER, &cxt);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

const auto CALLBACK_GET_ALL_BYTEARRAY = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = ((Context*) arg);
    const auto ckey = c->env->NewByteArray(kb);
    c->env->SetByteArrayRegion(ckey, 0, kb, (jbyte*) k);
    const auto cvalue = c->env->NewByteArray(vb);
    c->env->SetByteArrayRegion(cvalue, 0, vb, (jbyte*) v);
    c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
    c->env->DeleteLocalRef(ckey);
    c->env->DeleteLocalRef(cvalue);
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1all_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_ALL_BYTEARRAY, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_above(engine, (char*) ckey, ckeybytes, CALLBACK_GET_ALL_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_below(engine, (char*) ckey, ckeybytes, CALLBACK_GET_ALL_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_between(engine, (char*) ckey1, ckeybytes1, (char*) ckey2, ckeybytes2, CALLBACK_GET_ALL_BYTEARRAY, &cxt);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

const auto CALLBACK_GET_ALL_STRING = [](const char* k, size_t kb, const char* v, size_t vb, void *arg) -> int {
    const auto c = ((Context*) arg);
    const auto ckey = c->env->NewStringUTF(k);
    const auto cvalue = c->env->NewStringUTF(v);
    c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
    c->env->DeleteLocalRef(ckey);
    c->env->DeleteLocalRef(cvalue);
    return 0;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1all_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_all(engine, CALLBACK_GET_ALL_STRING, &cxt);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1above_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_above(engine, (char*) ckey, ckeybytes, CALLBACK_GET_ALL_STRING, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1below_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_below(engine, (char*) ckey, ckeybytes, CALLBACK_GET_ALL_STRING, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1get_1between_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_GET_ALL_STRING);
    Context cxt = CONTEXT;
    auto status = pmemkv_get_between(engine, (char*) ckey1, ckeybytes1, (char*) ckey2, ckeybytes2, CALLBACK_GET_ALL_STRING, &cxt);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
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

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1exists_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto result = pmemkv_exists(engine, (char*) ckey, ckeybytes) == PMEMKV_STATUS_OK;
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return result;
}

struct ContextGetBuffer {
    JNIEnv* env;
    int valuebytes;
    jobject value;
    jint result;
};

#define CONTEXT_GET_BUFFER {env, valuebytes, value, 0}

const auto CALLBACK_GET_BUFFER = [](const char* v, size_t vb, void *arg) {
    const auto c = ((ContextGetBuffer*) arg);
    if (vb > c->valuebytes) {
        c->env->ThrowNew(c->env->FindClass(EXCEPTION_CLASS), "ByteBuffer is too small");
    } else {
        char* cvalue = (char*) c->env->GetDirectBufferAddress(c->value);
        std::memcpy(cvalue, v, vb);
        c->result = vb;
    }
};

extern "C" JNIEXPORT jint JNICALL Java_io_pmem_pmemkv_Database_database_1get_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    ContextGetBuffer cxt = CONTEXT_GET_BUFFER;
    auto status = pmemkv_get(engine, (char*) ckey, keybytes, CALLBACK_GET_BUFFER, &cxt);
    if (status != PMEMKV_STATUS_OK && status != PMEMKV_STATUS_NOT_FOUND)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
    return cxt.result;
}

struct ContextGet {
    JNIEnv* env;
    jbyteArray result;
};

#define CONTEXT_GET {env, NULL}

const auto CALLBACK_GET = [](const char* v, size_t vb, void *arg)  {
    const auto c = ((ContextGet*) arg);
    c->result = c->env->NewByteArray(vb);
    c->env->SetByteArrayRegion(c->result, 0, vb, (jbyte*) v);
};

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_Database_database_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    ContextGet cxt = CONTEXT_GET;
    auto status = pmemkv_get(engine, (char*) ckey, ckeybytes, CALLBACK_GET, &cxt);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (status != PMEMKV_STATUS_OK && status != PMEMKV_STATUS_NOT_FOUND)
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

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_database_1put_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jbyteArray value) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cvalue = env->GetByteArrayElements(value, NULL);
    const auto cvaluebytes = env->GetArrayLength(value);
    const auto result = pmemkv_put(engine, (char*) ckey, ckeybytes, (char *) cvalue, cvaluebytes);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    env->ReleaseByteArrayElements(value, cvalue, JNI_ABORT);
    if (result != PMEMKV_STATUS_OK)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    auto engine = (pmemkv_db*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto result = pmemkv_remove(engine, ckey, keybytes);
    if (result != PMEMKV_STATUS_OK && result != PMEMKV_STATUS_NOT_FOUND)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
    return result == PMEMKV_STATUS_OK;
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_database_1remove_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    auto engine = (pmemkv_db*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto result = pmemkv_remove(engine, (char*) ckey, ckeybytes);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (result != PMEMKV_STATUS_OK && result != PMEMKV_STATUS_NOT_FOUND)
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
    return result == PMEMKV_STATUS_OK;
}
