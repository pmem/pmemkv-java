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
#include <iostream>

using pmemkv::KVEngine;

#define DO_LOG 0
#define LOG(msg) if (DO_LOG) std::cout << "[pmemkv-jni] " << msg << "\n"

#define EXCEPTION_CLASS "io/pmem/pmemkv/KVEngineException"

struct ContextStartFailure {
    std::string msg;
};

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1start
        (JNIEnv* env, jobject obj, jstring engine, jstring config) {
    const char* cengine = env->GetStringUTFChars(engine, NULL);
    const char* cconfig = env->GetStringUTFChars(config, NULL);

    pmemkv_config *cfg = pmemkv::pmemkv_config_new();
    int rv = pmemkv::pmemkv_config_from_json(cfg, cconfig);
    if (rv != 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Creating a pmemkv config from JSON string failed");

    ContextStartFailure cxt = {""};
    const auto cb = [](void* context, const char* engine, pmemkv_config *cfg, const char* msg) {
        const auto c = ((ContextStartFailure*) context);
        c->msg.append(msg);
    };
    const KVEngine* result = pmemkv::kvengine_start(&cxt, cengine, cfg, cb);
    pmemkv::pmemkv_config_delete(cfg);
    env->ReleaseStringUTFChars(engine, cengine);
    env->ReleaseStringUTFChars(config, cconfig);
    if (result == NULL) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), cxt.msg.c_str());
    return (jlong) result;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1stop
        (JNIEnv* env, jobject obj, jlong pointer) {
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_stop(engine);
}

struct Context {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
};

#define CONTEXT {env, callback, mid}

#define METHOD_ALL_BUFFER "(ILjava/nio/ByteBuffer;)V"
#define METHOD_ALL_BYTEARRAY "([B)V"
#define METHOD_ALL_STRING "(Ljava/lang/String;)V"
#define METHOD_EACH_BUFFER "(ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"
#define METHOD_EACH_BYTEARRAY "([B[B)V"
#define METHOD_EACH_STRING "(Ljava/lang/String;Ljava/lang/String;)V"

struct ContextAllBuffer {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
    int keybytes;
    char* key;
    jobject keybuf;
};

#define CONTEXT_ALL_BUFFER {env, callback, mid, -1, nullptr, nullptr}

const auto CALLBACK_ALL_BUFFER = [](void* context, int32_t kb, const char* k) {
    const auto c = ((ContextAllBuffer*) context);
    if (kb > c->keybytes) {
        if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
        c->keybytes = kb;
        c->key = new char[kb];
        c->keybuf = c->env->NewDirectByteBuffer(c->key, kb);
    }
    std::memcpy(c->key, k, kb);
    c->env->CallVoidMethod(c->callback, c->mid, kb, c->keybuf);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BUFFER);
    ContextAllBuffer cxt = CONTEXT_ALL_BUFFER;
    pmemkv::kvengine_all(engine, &cxt, CALLBACK_ALL_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BUFFER);
    ContextAllBuffer cxt = CONTEXT_ALL_BUFFER;
    pmemkv::kvengine_all_above(engine, &cxt, keybytes, ckey, CALLBACK_ALL_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BUFFER);
    ContextAllBuffer cxt = CONTEXT_ALL_BUFFER;
    pmemkv::kvengine_all_below(engine, &cxt, keybytes, ckey, CALLBACK_ALL_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BUFFER);
    ContextAllBuffer cxt = CONTEXT_ALL_BUFFER;
    pmemkv::kvengine_all_between(engine, &cxt, keybytes1, ckey1, keybytes2, ckey2, CALLBACK_ALL_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
}

const auto CALLBACK_ALL_BYTEARRAY = [](void* context, int32_t kb, const char* k) {
    const auto c = ((Context*) context);
    const auto ckey = c->env->NewByteArray(kb);
    c->env->SetByteArrayRegion(ckey, 0, kb, (jbyte*) k);
    c->env->CallVoidMethod(c->callback, c->mid, ckey);
    c->env->DeleteLocalRef(ckey);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all(engine, &cxt, CALLBACK_ALL_BYTEARRAY);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_above(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_ALL_BYTEARRAY);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_below(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_ALL_BYTEARRAY);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_between(engine, &cxt, ckeybytes1, (char*) ckey1, ckeybytes2, (char*) ckey2, CALLBACK_ALL_BYTEARRAY);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
}

const auto CALLBACK_ALL_STRING = [](void* context, int32_t kb, const char* k) {
    const auto c = ((Context*) context);
    const auto ckey = c->env->NewStringUTF(k);
    c->env->CallVoidMethod(c->callback, c->mid, ckey);
    c->env->DeleteLocalRef(ckey);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all(engine, &cxt, CALLBACK_ALL_STRING);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1above_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_above(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_ALL_STRING);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1below_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_below(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_ALL_STRING);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1between_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_ALL_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_all_between(engine, &cxt, ckeybytes1, (char*) ckey1, ckeybytes2, (char*) ckey2, CALLBACK_ALL_STRING);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count
        (JNIEnv* env, jobject obj, jlong pointer) {
    const auto engine = (KVEngine*) pointer;
    return pmemkv::kvengine_count(engine);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    return pmemkv::kvengine_count_above(engine, keybytes, ckey);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    return pmemkv::kvengine_count_below(engine, keybytes, ckey);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    return pmemkv::kvengine_count_between(engine, keybytes1, ckey1, keybytes2, ckey2);
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    auto result = pmemkv::kvengine_count_above(engine, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    auto result = pmemkv::kvengine_count_below(engine, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    auto result = pmemkv::kvengine_count_between(engine, ckeybytes1, (char*) ckey1, ckeybytes2, (char*) ckey2);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
    return result;
}

struct ContextEachBuffer {
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

#define CONTEXT_EACH_BUFFER {env, callback, mid, -1, nullptr, nullptr, -1, nullptr, nullptr}

const auto CALLBACK_EACH_BUFFER = [](void* context, int32_t kb, const char* k, int32_t vb, const char* v) {
    const auto c = ((ContextEachBuffer*) context);
    if (kb > c->keybytes) {
        if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
        c->keybytes = kb;
        c->key = new char[kb];
        c->keybuf = c->env->NewDirectByteBuffer(c->key, kb);
    }
    if (vb > c->valuebytes) {
        if (c->valuebuf != nullptr) c->env->DeleteLocalRef(c->valuebuf);
        c->valuebytes = vb;
        c->value = new char[vb];
        c->valuebuf = c->env->NewDirectByteBuffer(c->value, vb);
    }
    std::memcpy(c->key, k, kb);
    std::memcpy(c->value, v, vb);
    c->env->CallVoidMethod(c->callback, c->mid, kb, c->keybuf, vb, c->valuebuf);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BUFFER);
    ContextEachBuffer cxt = CONTEXT_EACH_BUFFER;
    pmemkv::kvengine_each(engine, &cxt, CALLBACK_EACH_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1above_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BUFFER);
    ContextEachBuffer cxt = CONTEXT_EACH_BUFFER;
    pmemkv::kvengine_each_above(engine, &cxt, keybytes, ckey, CALLBACK_EACH_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1below_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BUFFER);
    ContextEachBuffer cxt = CONTEXT_EACH_BUFFER;
    pmemkv::kvengine_each_below(engine, &cxt, keybytes, ckey, CALLBACK_EACH_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1between_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes1, jobject key1, jint keybytes2, jobject key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey1 = (char*) env->GetDirectBufferAddress(key1);
    const char* ckey2 = (char*) env->GetDirectBufferAddress(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BUFFER);
    ContextEachBuffer cxt = CONTEXT_EACH_BUFFER;
    pmemkv::kvengine_each_between(engine, &cxt, keybytes1, ckey1, keybytes2, ckey2, CALLBACK_EACH_BUFFER);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);
}

const auto CALLBACK_EACH_BYTEARRAY = [](void* context, int32_t kb, const char* k, int32_t vb, const char* v) {
    const auto c = ((Context*) context);
    const auto ckey = c->env->NewByteArray(kb);
    c->env->SetByteArrayRegion(ckey, 0, kb, (jbyte*) k);
    const auto cvalue = c->env->NewByteArray(vb);
    c->env->SetByteArrayRegion(cvalue, 0, vb, (jbyte*) v);
    c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
    c->env->DeleteLocalRef(ckey);
    c->env->DeleteLocalRef(cvalue);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each(engine, &cxt, CALLBACK_EACH_BYTEARRAY);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1above_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_above(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_EACH_BYTEARRAY);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1below_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_below(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_EACH_BYTEARRAY);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1between_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_BYTEARRAY);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_between(engine, &cxt, ckeybytes1, (char*) ckey1, ckeybytes2, (char*) ckey2, CALLBACK_EACH_BYTEARRAY);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
}

const auto CALLBACK_EACH_STRING = [](void* context, int32_t kb, const char* k, int32_t vb, const char* v) {
    const auto c = ((Context*) context);
    const auto ckey = c->env->NewStringUTF(k);
    const auto cvalue = c->env->NewStringUTF(v);
    c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
    c->env->DeleteLocalRef(ckey);
    c->env->DeleteLocalRef(cvalue);
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each(engine, &cxt, CALLBACK_EACH_STRING);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1above_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_above(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_EACH_STRING);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1below_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_below(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_EACH_STRING);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1between_1string
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key1, jbyteArray key2, jobject callback) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey1 = env->GetByteArrayElements(key1, NULL);
    const auto ckeybytes1 = env->GetArrayLength(key1);
    const auto ckey2 = env->GetByteArrayElements(key2, NULL);
    const auto ckeybytes2 = env->GetArrayLength(key2);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", METHOD_EACH_STRING);
    Context cxt = CONTEXT;
    pmemkv::kvengine_each_between(engine, &cxt, ckeybytes1, (char*) ckey1, ckeybytes2, (char*) ckey2, CALLBACK_EACH_STRING);
    env->ReleaseByteArrayElements(key1, ckey1, JNI_ABORT);
    env->ReleaseByteArrayElements(key2, ckey2, JNI_ABORT);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1exists_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    return pmemkv::kvengine_exists(engine, keybytes, ckey);
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1exists_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto result = pmemkv::kvengine_exists(engine, ckeybytes, (char*) ckey);
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

const auto CALLBACK_GET_BUFFER = [](void* context, int32_t vb, const char* v) {
    const auto c = ((ContextGetBuffer*) context);
    if (vb > c->valuebytes) {
        c->env->ThrowNew(c->env->FindClass(EXCEPTION_CLASS), "ByteBuffer is too small");
    } else {
        char* cvalue = (char*) c->env->GetDirectBufferAddress(c->value);
        std::memcpy(cvalue, v, vb);
        c->result = vb;
    }
};

extern "C" JNIEXPORT jint JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    ContextGetBuffer cxt = CONTEXT_GET_BUFFER;
    pmemkv::kvengine_get(engine, &cxt, keybytes, (char*) ckey, CALLBACK_GET_BUFFER);
    return cxt.result;
}

struct ContextGet {
    JNIEnv* env;
    jbyteArray result;
};

#define CONTEXT_GET {env, NULL}

const auto CALLBACK_GET = [](void* context, int32_t vb, const char* v) {
    const auto c = ((ContextGet*) context);
    c->result = c->env->NewByteArray(vb);
    c->env->SetByteArrayRegion(c->result, 0, vb, (jbyte*) v);
};

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    ContextGet cxt = CONTEXT_GET;
    pmemkv::kvengine_get(engine, &cxt, ckeybytes, (char*) ckey, CALLBACK_GET);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return cxt.result;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const char* cvalue = (char*) env->GetDirectBufferAddress(value);
    const auto result = pmemkv::kvengine_put(engine, keybytes, ckey, valuebytes, cvalue);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to put key");
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jbyteArray value) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cvalue = env->GetByteArrayElements(value, NULL);
    const auto cvaluebytes = env->GetArrayLength(value);
    const auto result = pmemkv::kvengine_put(engine, ckeybytes, (char*) ckey, cvaluebytes, (char*) cvalue);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    env->ReleaseByteArrayElements(value, cvalue, JNI_ABORT);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to put key");
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {
    const auto engine = (KVEngine*) pointer;
    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto result = pmemkv::kvengine_remove(engine, keybytes, ckey);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to remove key");
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1remove_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {
    const auto engine = (KVEngine*) pointer;
    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto result = pmemkv::kvengine_remove(engine, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to remove key");
    return result;
}
