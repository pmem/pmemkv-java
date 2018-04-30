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
#define LOG(msg) if (DO_LOG) std::cout << "[kvengine] " << msg << "\n"

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

extern "C" JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1get
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);

    thread_local jsize cvaluelimit = 64;
    thread_local auto cvalue = new char[cvaluelimit];
    int32_t cvaluebytes = 0;

    LOG("Get for key= " << std::string((char*) ckey, ckeybytes)
                        << ", limit=" << to_string(cvaluelimit));
    int8_t res = pmemkv::kvengine_get((KVEngine*) pointer, cvaluelimit, ckeybytes, &cvaluebytes,
                                      (char*) ckey, cvalue);

    if (res < 0) {
        LOG("   resizing buffer: " << to_string(cvaluelimit)
                                   << " to " << to_string(cvaluebytes));
        // todo validate that cvaluebytes is in a safe range?
        delete cvalue;
        cvaluelimit = cvaluebytes;
        cvalue = new char[cvaluelimit];
        res = pmemkv::kvengine_get((KVEngine*) pointer, cvaluelimit, ckeybytes, &cvaluebytes,
                                   (char*) ckey, cvalue);
    }

    if (res == 0) {
        LOG("   could not find key");
        env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
        return NULL;
    } else if (res > 0) {
        LOG("   found value, copying");
        env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
        jbyteArray result = env->NewByteArray(cvaluebytes);
        env->SetByteArrayRegion(result, 0, cvaluebytes, (jbyte*) cvalue);
        return result;
    } else {
        string msg;
        msg.append("unable to get key: ");
        msg.append(std::string((char*) ckey, ckeybytes));
        env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), msg.c_str());
        return NULL;
    }

}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_KVEngine_kvengine_1put
        (JNIEnv* env, jobject obj, jlong pointer, jbyteArray key, jbyteArray value) {

    const auto ckey = env->GetByteArrayElements(key, NULL);
    const auto ckeybytes = env->GetArrayLength(key);
    const auto cvalue = env->GetByteArrayElements(value, NULL);
    auto cvaluebytes = env->GetArrayLength(value);

    int8_t res = pmemkv::kvengine_put((KVEngine*) pointer, ckeybytes, &cvaluebytes,
                                      (char*) ckey, (char*) cvalue);

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

    env->ReleaseByteArrayElements(key, ckey, JNI_ABORT);

}
