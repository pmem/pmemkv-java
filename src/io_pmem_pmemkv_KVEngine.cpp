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

#include <jni.h>
#include <libpmemkv.h>
#include <iostream>

using pmemkv::KVEngine;

#define DO_LOG 0
#define LOG(msg) if (DO_LOG) std::cout << "[pmemkv-jni] " << msg << "\n"

#define EXCEPTION_CLASS "io/pmem/pmemkv/KVEngineException"

struct StartFailureCallbackContext {
    string msg;
};

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1start
        (JNIEnv* env, jobject obj, jstring engine, jstring config) {

    const auto cb = [](void* context, const char* engine, const char* config, const char* msg) {
        const auto c = ((StartFailureCallbackContext*) context);
        c->msg.append(msg);
    };

    const char* cengine = env->GetStringUTFChars(engine, NULL);
    const char* cconfig = env->GetStringUTFChars(config, NULL);
    StartFailureCallbackContext cxt = {""};
    const KVEngine* result = pmemkv::kvengine_start(&cxt, cengine, cconfig, cb);
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

struct AllBuffersCallbackContext {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
    int keybytes;
    char* key;
    jobject keybuf;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1buffers
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key) {
        const auto c = ((AllBuffersCallbackContext*) context);
        if (keybytes > c->keybytes) {
            if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
            c->keybytes = keybytes;
            c->key = new char[keybytes];
            c->keybuf = c->env->NewDirectByteBuffer(c->key, keybytes);
        }
        memcpy(c->key, key, keybytes);
        c->env->CallVoidMethod(c->callback, c->mid, keybytes, c->keybuf);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(ILjava/nio/ByteBuffer;)V");
    AllBuffersCallbackContext cxt = {env, callback, mid, -1, nullptr, nullptr};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_all(engine, &cxt, cb);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);

}

struct CallbackContext {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1bytearrays
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewByteArray(keybytes);
        c->env->SetByteArrayRegion(ckey, 0, keybytes, (jbyte*) key);
        c->env->CallVoidMethod(c->callback, c->mid, ckey);
        c->env->DeleteLocalRef(ckey);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "([B)V");
    CallbackContext cxt = {env, callback, mid};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_all(engine, &cxt, cb);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1strings
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewStringUTF(key);
        c->env->CallVoidMethod(c->callback, c->mid, ckey);
        c->env->DeleteLocalRef(ckey);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(Ljava/lang/String;)V");
    CallbackContext cxt = {env, callback, mid};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_all(engine, &cxt, cb);

}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count
        (JNIEnv* env, jobject obj, jlong pointer) {

    const auto engine = (KVEngine*) pointer;
    return pmemkv::kvengine_count(engine);

}

struct EachBufferCallbackContext {
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

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key, int32_t valuebytes, const char* value) {
        const auto c = ((EachBufferCallbackContext*) context);
        if (keybytes > c->keybytes) {
            if (c->keybuf != nullptr) c->env->DeleteLocalRef(c->keybuf);
            c->keybytes = keybytes;
            c->key = new char[keybytes];
            c->keybuf = c->env->NewDirectByteBuffer(c->key, keybytes);
        }
        if (valuebytes > c->valuebytes) {
            if (c->valuebuf != nullptr) c->env->DeleteLocalRef(c->valuebuf);
            c->valuebytes = valuebytes;
            c->value = new char[valuebytes];
            c->valuebuf = c->env->NewDirectByteBuffer(c->value, valuebytes);
        }
        memcpy(c->key, key, keybytes);
        memcpy(c->value, value, valuebytes);
        c->env->CallVoidMethod(c->callback, c->mid, keybytes, c->keybuf, valuebytes, c->valuebuf);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V");
    EachBufferCallbackContext cxt = {env, callback, mid, -1, nullptr, nullptr, -1, nullptr, nullptr};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_each(engine, &cxt, cb);
    if (cxt.keybuf != nullptr) env->DeleteLocalRef(cxt.keybuf);
    if (cxt.valuebuf != nullptr) env->DeleteLocalRef(cxt.valuebuf);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1bytearray
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key, int32_t valuebytes, const char* value) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewByteArray(keybytes);
        c->env->SetByteArrayRegion(ckey, 0, keybytes, (jbyte*) key);
        const auto cvalue = c->env->NewByteArray(valuebytes);
        c->env->SetByteArrayRegion(cvalue, 0, valuebytes, (jbyte*) value);
        c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
        c->env->DeleteLocalRef(ckey);
        c->env->DeleteLocalRef(cvalue);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "([B[B)V");
    CallbackContext cxt = {env, callback, mid};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_each(engine, &cxt, cb);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    const auto cb = [](void* context, int32_t keybytes, const char* key, int32_t valuebytes, const char* value) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewStringUTF(key);
        const auto cvalue = c->env->NewStringUTF(value);
        c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
        c->env->DeleteLocalRef(ckey);
        c->env->DeleteLocalRef(cvalue);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(Ljava/lang/String;Ljava/lang/String;)V");
    CallbackContext cxt = {env, callback, mid};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_each(engine, &cxt, cb);

}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1exists_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {

    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto engine = (KVEngine*) pointer;
    return pmemkv::kvengine_exists(engine, keybytes, ckey);

}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1exists_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto engine = (KVEngine*) pointer;
    const auto result = pmemkv::kvengine_exists(engine, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return result;

}

struct GetBufferCallbackContext {
    JNIEnv* env;
    int valuebytes;
    jobject value;
    jint result;
};

extern "C" JNIEXPORT jint JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {

    const auto cb = [](void* context, int32_t vb, const char* v) {
        const auto c = ((GetBufferCallbackContext*) context);
        if (vb > c->valuebytes) {
            c->env->ThrowNew(c->env->FindClass(EXCEPTION_CLASS), "ByteBuffer is too small");
        } else {
            char* cvalue = (char*) c->env->GetDirectBufferAddress(c->value);
            memcpy(cvalue, v, vb);
            c->result = vb;
        }
    };

    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    GetBufferCallbackContext cxt = {env, valuebytes, value, 0};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_get(engine, &cxt, keybytes, (char*) ckey, cb);
    return cxt.result;

}

struct GetCallbackContext {
    JNIEnv* env;
    jbyteArray result;
};

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto cb = [](void* context, int32_t valuebytes, const char* value) {
        const auto c = ((GetCallbackContext*) context);
        c->result = c->env->NewByteArray(valuebytes);
        c->env->SetByteArrayRegion(c->result, 0, valuebytes, (jbyte*) value);
    };

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    GetCallbackContext cxt = {env, NULL};
    const auto engine = (KVEngine*) pointer;
    pmemkv::kvengine_get(engine, &cxt, ckeybytes, (char*) ckey, cb);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return cxt.result;

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key, jint valuebytes, jobject value) {

    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const char* cvalue = (char*) env->GetDirectBufferAddress(value);
    const auto engine = (KVEngine*) pointer;
    const auto result = pmemkv::kvengine_put(engine, keybytes, ckey, valuebytes, cvalue);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to put key");

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jbyteArray value) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cvalue = env->GetByteArrayElements(value, NULL);
    const auto cvaluebytes = env->GetArrayLength(value);
    const auto engine = (KVEngine*) pointer;
    const auto result = pmemkv::kvengine_put(engine, ckeybytes, (char*) ckey, cvaluebytes, (char*) cvalue);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    env->ReleaseByteArrayElements(value, cvalue, JNI_ABORT);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to put key");

}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1remove_1buffer
        (JNIEnv* env, jobject obj, jlong pointer, jint keybytes, jobject key) {

    const char* ckey = (char*) env->GetDirectBufferAddress(key);
    const auto engine = (KVEngine*) pointer;
    const auto result = pmemkv::kvengine_remove(engine, keybytes, ckey);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to remove key");
    return result;

}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1remove_1bytes
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto engine = (KVEngine*) pointer;
    const auto result = pmemkv::kvengine_remove(engine, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    if (result < 0) env->ThrowNew(env->FindClass(EXCEPTION_CLASS), "Unable to remove key");
    return result;

}
