// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.internal.GetKeysBuffersJNICallback;
import io.pmem.pmemkv.internal.GetAllBufferJNICallback;
import io.pmem.pmemkv.Pmemkv;

import java.nio.ByteBuffer;

public class Database implements Pmemkv<ByteBuffer, ByteBuffer> {
    private ByteBuffer getDirectBuffer(ByteBuffer buf) {
      if (buf.isDirect()) {
        return buf;
      }
      ByteBuffer directBuffer = ByteBuffer.allocateDirect(buf.capacity());
      directBuffer.put(buf);
      return directBuffer;
    }

    public Database(String engine, String config) {
        pointer = database_start(engine, config);
    }

    public void stop() {
        if (!stopped) {
            stopped = true;
            database_stop(pointer);
        }
    }

    public boolean stopped() {
        return stopped;
    }

    public void getKeys(KeyCallback<ByteBuffer> callback) {
        database_get_keys_buffer(pointer, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysAbove(ByteBuffer key, KeyCallback<ByteBuffer> callback) {
        ByteBuffer direct_key = getDirectBuffer(key);
        database_get_keys_above_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBelow(ByteBuffer key, KeyCallback<ByteBuffer> callback) {
        ByteBuffer direct_key = getDirectBuffer(key);
        database_get_keys_below_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBetween(ByteBuffer key1, ByteBuffer key2, KeyCallback<ByteBuffer> callback) {
        ByteBuffer direct_key1 = getDirectBuffer(key1);
        ByteBuffer direct_key2 = getDirectBuffer(key2);
        database_get_keys_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public long countAll() {
        return database_count_all(pointer);
    }

    public long countAbove(ByteBuffer key) {
        ByteBuffer direct_key = getDirectBuffer(key);
        return database_count_above_buffer(pointer, direct_key.position(), direct_key);
    }

    public long countBelow(ByteBuffer key) {
        ByteBuffer direct_key = getDirectBuffer(key);
        return database_count_below_buffer(pointer, direct_key.position(), direct_key);
    }

    public long countBetween(ByteBuffer key1, ByteBuffer key2) {
        ByteBuffer direct_key1 = getDirectBuffer(key1);
        ByteBuffer direct_key2 = getDirectBuffer(key2);
        return database_count_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2);
    }

    public void getAll(KeyValueCallback<ByteBuffer, ByteBuffer>  callback) {
        database_get_all_buffer(pointer, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getAbove(ByteBuffer key, KeyValueCallback<ByteBuffer, ByteBuffer>  callback) {
        ByteBuffer direct_key = getDirectBuffer(key);
        database_get_above_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBelow(ByteBuffer key, KeyValueCallback<ByteBuffer, ByteBuffer>  callback) {
        ByteBuffer direct_key = getDirectBuffer(key);
        database_get_below_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBetween(ByteBuffer key1, ByteBuffer key2, KeyValueCallback<ByteBuffer, ByteBuffer>  callback) {
        ByteBuffer direct_key1 = getDirectBuffer(key1);
        ByteBuffer direct_key2 = getDirectBuffer(key2);
        database_get_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public boolean exists(ByteBuffer key) {
        ByteBuffer direct_key = getDirectBuffer(key);
        return database_exists_buffer(pointer, direct_key.position(), direct_key);
    }

    public void get(ByteBuffer key, ValueCallback<ByteBuffer> callback) {
        ByteBuffer direct_key = getDirectBuffer(key);
        database_get_buffer_with_callback(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb) ));
    }

    public ByteBuffer getCopy(ByteBuffer key) {
        byte value[];
        ByteBuffer direct_key = getDirectBuffer(key);
        //TODO change type of exception to one related to PMEMKV_STATUS_NOT_FOUND
        // when implemented
        try {
            value = database_get_bytes(pointer, direct_key.position(), direct_key);
        } catch (DatabaseException kve) {
            return null;
        }
        return ByteBuffer.wrap(value);
    }

    public void put(ByteBuffer key, ByteBuffer value) {
          ByteBuffer direct_key = getDirectBuffer(key);
          ByteBuffer direct_value = getDirectBuffer(value);
          try {
              database_put_buffer(pointer, direct_key.position(), direct_key, direct_value.position(), direct_value);
          } catch (DatabaseException kve) {
              kve.setKey(key);
              throw kve;
          }
    }

    public boolean remove(ByteBuffer key) {
        ByteBuffer direct_key = getDirectBuffer(key);
        try {
            return database_remove_buffer(pointer, direct_key.position(), direct_key);
        } catch (DatabaseException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    private final long pointer;
    private boolean stopped;

    // JNI METHODS --------------------------------------------------------------------------------

    private native long database_start(String engine, String config);
    private native void database_stop(long ptr);
    private native void database_get_keys_buffer(long ptr, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_above_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_below_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2, GetKeysBuffersJNICallback cb);
    private native long database_count_all(long ptr);
    private native long database_count_above_buffer(long ptr, int kb, ByteBuffer k);
    private native long database_count_below_buffer(long ptr, int kb, ByteBuffer k);
    private native long database_count_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2);
    private native void database_get_all_buffer(long ptr, GetAllBufferJNICallback cb);
    private native void database_get_above_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);
    private native void database_get_below_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);
    private native void database_get_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2, GetAllBufferJNICallback cb);
    private native boolean database_exists_buffer(long ptr, int kb, ByteBuffer k);
    private native void database_get_buffer_with_callback(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native byte[] database_get_bytes(long ptr, int kb, ByteBuffer k);
    private native void database_put_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);
    private native boolean database_remove_buffer(long ptr, int kb, ByteBuffer k);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
