// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

#include <jni.h>
#include <libpmemkv.h>

#define EXCEPTION_CLASS "io/pmem/pmemkv/DatabaseException"

extern "C" JNIEXPORT jlong JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1new
  (JNIEnv *env, jobject) {
    auto cfg = pmemkv_config_new();
    if (cfg == nullptr) {
        env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());
        return 0;
    }

    return (jlong) cfg;
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1delete
  (JNIEnv *, jobject cfg){
    pmemkv_config_delete((pmemkv_config *) cfg);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1put_1int
  (JNIEnv *env, jobject, jlong cfg, jstring jkey, jlong value){
    const char* key = env->GetStringUTFChars(jkey, NULL);

    auto status = pmemkv_config_put_int64((pmemkv_config *) cfg, key, (int64_t) value);
    if (status != PMEMKV_STATUS_OK)
      env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());

    env->ReleaseStringUTFChars(jkey, key);
}

extern "C" JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024Builder_config_1put_1string
  (JNIEnv *env, jobject, jlong cfg, jstring jkey, jstring jvalue){
    const char* key = env->GetStringUTFChars(jkey, NULL);
    const char* value = env->GetStringUTFChars(jvalue, NULL);

    auto status = pmemkv_config_put_string((pmemkv_config *) cfg, key, value);
    if (status != PMEMKV_STATUS_OK)
      env->ThrowNew(env->FindClass(EXCEPTION_CLASS), pmemkv_errormsg());

    env->ReleaseStringUTFChars(jkey, key);
    env->ReleaseStringUTFChars(jvalue, value);
}
