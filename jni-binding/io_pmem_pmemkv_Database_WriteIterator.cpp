// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

// #include <io_pmem_pmemkv_Database_WriteIterator.h>
#include <common.h>

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_new_write_iterator
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1new_1write_1iterator(JNIEnv *env, jobject, jlong db_pointer)
{
  auto engine = reinterpret_cast<pmem::kv::db *>(db_pointer);
  pmem::kv::result<pmem::kv::db::write_iterator> res = engine->new_write_iterator();

  if (res.is_ok()) {
    pmem::kv::db::write_iterator &w_it = res.get_value();
    pmem::kv::db::write_iterator *ptr = new pmem::kv::db::write_iterator(std::move(w_it));
    return (jlong)ptr;
  } else {
    PmemkvJavaException(env).ThrowException(res.get_status());
  }
  return 0;
}

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek
  (JNIEnv *, jobject, jlong, jstring);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_lower
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1lower
  (JNIEnv *, jobject, jlong, jstring);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_lower_eq
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1lower_1eq
  (JNIEnv *, jobject, jlong, jstring);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_higher
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1higher
  (JNIEnv *, jobject, jlong, jstring);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_higher_eq
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1higher_1eq
  (JNIEnv *, jobject, jlong, jstring);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_to_first
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1to_1first
  (JNIEnv *env, jobject, jlong ptr) {
    auto w_it = reinterpret_cast<pmem::kv::db::write_iterator*>(ptr);
    auto status = w_it->seek_to_first();

    if (status == pmem::kv::status::OK or status == pmem::kv::status::NOT_FOUND) {
      return status == pmem::kv::status::OK;
    }
    PmemkvJavaException(env).ThrowException(status);
    return false;
}

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_seek_to_last
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1seek_1to_1last
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_is_next
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1is_1next
  (JNIEnv *env, jobject, jlong ptr) {
    auto w_it = reinterpret_cast<pmem::kv::db::write_iterator*>(ptr);
    auto status = w_it->is_next();

    if (status == pmem::kv::status::OK or status == pmem::kv::status::NOT_FOUND) {
      return status == pmem::kv::status::OK;
    }
    PmemkvJavaException(env).ThrowException(status);
    return false;
}

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_next
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1next
  (JNIEnv *env, jobject, jlong ptr) {
    auto w_it = reinterpret_cast<pmem::kv::db::write_iterator*>(ptr);
    auto status = w_it->next();

    if (status == pmem::kv::status::OK or status == pmem::kv::status::NOT_FOUND) {
      return status == pmem::kv::status::OK;
    }
    PmemkvJavaException(env).ThrowException(status);
    return false;
}

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_prev
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1prev
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_key
 * Signature: (J)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1key
  (JNIEnv *env, jobject, jlong ptr) {
  auto w_it = reinterpret_cast<pmem::kv::db::write_iterator*>(ptr);
  auto res = w_it->key();
  if (res.is_ok()) {
    auto &key_value = res.get_value();
    auto key_size = key_value.size();
    jobject key_jbuffer = env->NewDirectByteBuffer(const_cast<char*>(key_value.data()), key_size);
    return key_jbuffer;
  }

  PmemkvJavaException(env).ThrowException(res.get_status());
  return nullptr;
}

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_read_range
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1read_1range
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_commit
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1commit
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_pmem_pmemkv_Database_WriteIterator
 * Method:    iterator_abort
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_io_pmem_pmemkv_Database_00024WriteIterator_iterator_1abort
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
