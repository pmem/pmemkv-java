// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

#ifndef HEADER_COMMON_H
#define HEADER_COMMON_H

#include <jni.h>
#include <libpmemkv.hpp>
#include <unordered_map>

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

#define KEY_CALLBACK_NAME "keyCallbackWrapper"
#define VALUE_CALLBACK_NAME "valueCallbackWrapper"
#define KEY_VALUE_CALLBACK_NAME "keyValueCallbackWrapper"
#define KEY_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyCallback;ILjava/nio/ByteBuffer;)V"
#define VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/ValueCallback;ILjava/nio/ByteBuffer;)V"
#define KEY_VALUE_CALLBACK_SIG "(Lio/pmem/pmemkv/Database;Lio/pmem/pmemkv/KeyValueCallback;ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;)V"

#endif // HEADER_COMMON_H