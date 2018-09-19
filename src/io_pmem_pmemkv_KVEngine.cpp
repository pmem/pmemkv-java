/*
 * Copyright 2017-2018, Intel Corporation
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

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1open
        (JNIEnv* env, jobject obj, jstring engine, jstring path, jlong size) {

    const char* cengine = env->GetStringUTFChars(engine, NULL);
    const char* cpath = env->GetStringUTFChars(path, NULL);

    KVEngine* result = pmemkv::kvengine_open(cengine, cpath, (size_t) size);

    env->ReleaseStringUTFChars(engine, cengine);
    env->ReleaseStringUTFChars(path, cpath);
    if (result == NULL) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "unable to open persistent pool");
    }
    return (jlong) result;

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1close
        (JNIEnv* env, jobject obj, jlong pointer) {

    pmemkv::kvengine_close((KVEngine*) pointer);

}

struct CallbackContext {
    JNIEnv* env;
    jobject callback;
    jmethodID mid;
};

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewByteArray(keybytes);
        c->env->SetByteArrayRegion(ckey, 0, keybytes, (jbyte*) key);
        c->env->CallVoidMethod(c->callback, c->mid, ckey);
        c->env->DeleteLocalRef(ckey);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "([B)V");
    CallbackContext cxt = {env, callback, mid};
    pmemkv::kvengine_all((KVEngine*) pointer, &cxt, cb);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1all_1strings
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewStringUTF(key);
        c->env->CallVoidMethod(c->callback, c->mid, ckey);
        c->env->DeleteLocalRef(ckey);
    };

    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(Ljava/lang/String;)V");
    CallbackContext cxt = {env, callback, mid};
    pmemkv::kvengine_all((KVEngine*) pointer, &cxt, cb);

}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count
        (JNIEnv* env, jobject obj, jlong pointer) {

    return pmemkv::kvengine_count((KVEngine*) pointer);

}

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1count_1like
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray pattern) {

    const auto cpattern = env->GetByteArrayElements(pattern, NULL);
    const auto cpatternbytes = env->GetArrayLength(pattern);
    auto result = pmemkv::kvengine_count_like((KVEngine*) pointer, cpatternbytes, (char*) cpattern);
    env->ReleaseByteArrayElements(pattern, cpattern, JNI_ABORT);
    return result;

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key,
                 int32_t valuebytes, const char* value) {
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
    pmemkv::kvengine_each((KVEngine*) pointer, &cxt, cb);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1like
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray pattern, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key,
                 int32_t valuebytes, const char* value) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewByteArray(keybytes);
        c->env->SetByteArrayRegion(ckey, 0, keybytes, (jbyte*) key);
        const auto cvalue = c->env->NewByteArray(valuebytes);
        c->env->SetByteArrayRegion(cvalue, 0, valuebytes, (jbyte*) value);
        c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
        c->env->DeleteLocalRef(ckey);
        c->env->DeleteLocalRef(cvalue);
    };

    const auto cpattern = env->GetByteArrayElements(pattern, NULL);
    const auto cpatternbytes = env->GetArrayLength(pattern);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "([B[B)V");
    CallbackContext cxt = {env, callback, mid};
    pmemkv::kvengine_each_like((KVEngine*) pointer, cpatternbytes, (char*) cpattern, &cxt, cb);
    env->ReleaseByteArrayElements(pattern, cpattern, JNI_ABORT);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1string
        (JNIEnv* env, jobject obj, jlong pointer, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key,
                 int32_t valuebytes, const char* value) {
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
    pmemkv::kvengine_each((KVEngine*) pointer, &cxt, cb);

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1each_1string_1like
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray pattern, jobject callback) {

    auto cb = [](void* context, int32_t keybytes, const char* key,
                 int32_t valuebytes, const char* value) {
        const auto c = ((CallbackContext*) context);
        const auto ckey = c->env->NewStringUTF(key);
        const auto cvalue = c->env->NewStringUTF(value);
        c->env->CallVoidMethod(c->callback, c->mid, ckey, cvalue);
        c->env->DeleteLocalRef(ckey);
        c->env->DeleteLocalRef(cvalue);
    };

    const auto cpattern = env->GetByteArrayElements(pattern, NULL);
    const auto cpatternbytes = env->GetArrayLength(pattern);
    const auto cls = env->GetObjectClass(callback);
    const auto mid = env->GetMethodID(cls, "process", "(Ljava/lang/String;Ljava/lang/String;)V");
    CallbackContext cxt = {env, callback, mid};
    pmemkv::kvengine_each_like((KVEngine*) pointer, cpatternbytes, (char*) cpattern, &cxt, cb);
    env->ReleaseByteArrayElements(pattern, cpattern, JNI_ABORT);

}

extern "C" JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1exists
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    auto result = pmemkv::kvengine_exists((KVEngine*) pointer, ckeybytes, (char*) ckey);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return result;

}

struct GetCallbackContext {
    JNIEnv* env;
    jbyteArray result;
};

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    auto cb = [](void* context, int32_t valuebytes, const char* value) {
        const auto c = ((GetCallbackContext*) context);
        c->result = c->env->NewByteArray(valuebytes);
        c->env->SetByteArrayRegion(c->result, 0, valuebytes, (jbyte*) value);
    };

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    GetCallbackContext cxt = {env, NULL};
    pmemkv::kvengine_get((KVEngine*) pointer, &cxt, ckeybytes, (char*) ckey, cb);
    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    return cxt.result;

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jbyteArray value) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cvalue = env->GetByteArrayElements(value, NULL);
    auto cvaluebytes = env->GetArrayLength(value);

    int8_t res = pmemkv::kvengine_put((KVEngine*) pointer, ckeybytes, (char*) ckey,
                                      cvaluebytes, (char*) cvalue);

    env->ReleaseByteArrayElements(value, cvalue, JNI_ABORT);
    if (res != 1) {
        string msg;
        msg.append("unable to put key: ");
        msg.append(std::string((char*) ckey, ckeybytes));
        env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), msg.c_str());
    } else {
        env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
    }

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1remove
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);

    pmemkv::kvengine_remove((KVEngine*) pointer, ckeybytes, (char*) ckey);
    // todo should be checking return value!

    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);

}
