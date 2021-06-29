// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

#include <common.h>

/* Helper function for seek methods */
template <typename Function>
jboolean boilerplate_seek(JNIEnv *env, jlong ptr, Function &&func, jobject key = nullptr) {
  auto r_it = reinterpret_cast<pmem::kv::db::read_iterator*>(ptr);
  const char* ckey = (key ? reinterpret_cast<char*>(env->GetDirectBufferAddress(key)) : nullptr);
  pmem::kv::status status = func(r_it, ckey);

  if (status == pmem::kv::status::OK || status == pmem::kv::status::NOT_FOUND) {
    return status == pmem::kv::status::OK;
  }
  PmemkvJavaException(env).ThrowException(status);
  return false;
}

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_new_read_iterator
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1new_1read_1iterator(JNIEnv *env, jobject, jlong db_pointer) {
  auto engine = reinterpret_cast<pmem::kv::db *>(db_pointer);
  pmem::kv::result<pmem::kv::db::read_iterator> res = engine->new_read_iterator();

  if (res.is_ok()) {
    pmem::kv::db::read_iterator &r_it = res.get_value();
    pmem::kv::db::read_iterator *ptr = new pmem::kv::db::read_iterator(std::move(r_it));
    return reinterpret_cast<jlong>(ptr);
  } else {
    PmemkvJavaException(env).ThrowException(res.get_status());
  }
  return 0;
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek
  (JNIEnv *env, jobject, jlong ptr, jobject key) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
	  return r_it->seek(ckey);
  }, key);
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_lower
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1lower
  (JNIEnv *env, jobject, jlong ptr, jobject key) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
	  return r_it->seek_lower(ckey);
  }, key);
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_lower_eq
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1lower_1eq
  (JNIEnv *env, jobject, jlong ptr, jobject key) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
	  return r_it->seek_lower_eq(ckey);
  }, key);
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_higher
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1higher
  (JNIEnv *env, jobject, jlong ptr, jobject key) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
	  return r_it->seek_higher(ckey);
  }, key);
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_higher_eq
 * Signature: (JLjava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1higher_1eq
  (JNIEnv *env, jobject, jlong ptr, jobject key) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
	  return r_it->seek_higher_eq(ckey);
  }, key);
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_to_first
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1to_1first
  (JNIEnv *env, jobject, jlong ptr) {
    return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
      return r_it->seek_to_first();
    });
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_seek_to_last
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1seek_1to_1last
  (JNIEnv *env, jobject, jlong ptr) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
    return r_it->seek_to_last();
  });
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_is_next
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1is_1next
  (JNIEnv *env, jobject, jlong ptr) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
    return r_it->is_next();
  });
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_next
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1next
  (JNIEnv *env, jobject, jlong ptr) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
    return r_it->next();
  });
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_prev
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1prev
  (JNIEnv *env, jobject, jlong ptr) {
  return boilerplate_seek(env, ptr, [](pmem::kv::db::read_iterator *r_it, const char* ckey) {
    return r_it->prev();
  });
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_key
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1key
  (JNIEnv *env, jobject, jlong ptr) {
  auto r_it = reinterpret_cast<pmem::kv::db::read_iterator*>(ptr);
  auto res = r_it->key();
  if (res.is_ok()) {
    auto &key = res.get_value();
    auto key_jbuffer = env->NewDirectByteBuffer(const_cast<char*>(key.data()), key.size());
    if (env->ExceptionCheck() == JNI_TRUE)
        return nullptr;  // Propagate exception
    return key_jbuffer;
  }

  PmemkvJavaException(env).ThrowException(res.get_status());
  return nullptr;
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_value
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1value
  (JNIEnv *env, jobject, jlong ptr) {
  auto r_it = reinterpret_cast<pmem::kv::db::read_iterator*>(ptr);
  auto res = r_it->read_range();
  if (res.is_ok()) {
    auto &value = res.get_value();
    auto val_jbuffer = env->NewDirectByteBuffer(const_cast<char*>(value.data()), value.size());
    if (env->ExceptionCheck() == JNI_TRUE)
        return nullptr;  // Propagate exception
    return val_jbuffer;
  }

  PmemkvJavaException(env).ThrowException(res.get_status());
  return nullptr;
}

/*
 * Class:     io_pmem_pmemkv_Database_ReadIterator
 * Method:    iterator_close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024ReadIterator_iterator_1close
  (JNIEnv *, jobject, jlong ptr) {
    auto r_it = reinterpret_cast<pmem::kv::db::read_iterator*>(ptr);
    delete r_it;
}

#ifdef __cplusplus
}
#endif
