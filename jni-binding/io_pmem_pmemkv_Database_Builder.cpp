// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

#include <jni.h>
#include <libpmemkv.h>
#include <libpmemkv_json_config.h>

#define EXCEPTION_CLASS "io/pmem/pmemkv/BuilderException"

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1new
  (JNIEnv *env, jobject) {
    auto cfg = pmemkv_config_new();
    if (cfg == nullptr) {
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
        return 0;
    }
    return reinterpret_cast<jlong>(cfg);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1delete
  (JNIEnv *, jobject, jlong cfg) {
    pmemkv_config_delete(reinterpret_cast<pmemkv_config*>(cfg));
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1put_1int
  (JNIEnv *env, jobject, jlong cfg, jstring jkey, jlong value) {
    const char* key = env->GetStringUTFChars(jkey, NULL);
    auto status = pmemkv_config_put_int64(reinterpret_cast<pmemkv_config*>(cfg), key, (int64_t) value);
    if (status != PMEMKV_STATUS_OK)
      env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());

    env->ReleaseStringUTFChars(jkey, key);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1put_1string
  (JNIEnv *env, jobject, jlong cfg, jstring jkey, jstring jvalue) {
    const char* key = env->GetStringUTFChars(jkey, NULL);
    const char* value = env->GetStringUTFChars(jvalue, NULL);

    auto status = pmemkv_config_put_string(reinterpret_cast<pmemkv_config*>(cfg), key, value);
    if (status != PMEMKV_STATUS_OK)
      env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());

    env->ReleaseStringUTFChars(jkey, key);
    env->ReleaseStringUTFChars(jvalue, value);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1from_1json
  (JNIEnv *env, jobject, jlong cfg, jstring jjson) {
    const char* cjson = env->GetStringUTFChars(jjson, NULL);

    auto status = pmemkv_config_from_json(reinterpret_cast<pmemkv_config*>(cfg), cjson);
    if (status != PMEMKV_STATUS_OK)
      env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_config_from_json_errormsg());

    env->ReleaseStringUTFChars(jjson, cjson);
}
