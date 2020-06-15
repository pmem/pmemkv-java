// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.internal.GetKeysBuffersJNICallback;
import io.pmem.pmemkv.internal.GetAllBufferJNICallback;

import java.nio.ByteBuffer;

public class Database {

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

    public void getKeys(GetKeysBuffersCallback callback) {
        database_get_keys_buffer(pointer, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysAbove(ByteBuffer key, GetKeysBuffersCallback callback) {
        database_get_keys_above_buffer(pointer, key.position(), key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBelow(ByteBuffer key, GetKeysBuffersCallback callback) {
        database_get_keys_below_buffer(pointer, key.position(), key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBetween(ByteBuffer key1, ByteBuffer key2, GetKeysBuffersCallback callback) {
        database_get_keys_between_buffer(pointer, key1.position(), key1, key2.position(), key2, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public long countAll() {
        return database_count_all(pointer);
    }

    public long countAbove(ByteBuffer key) {
        return database_count_above_buffer(pointer, key.position(), key);
    }

    public long countBelow(ByteBuffer key) {
        return database_count_below_buffer(pointer, key.position(), key);
    }

    public long countBetween(ByteBuffer key1, ByteBuffer key2) {
        return database_count_between_buffer(pointer, key1.position(), key1, key2.position(), key2);
    }

    public void getAll(GetAllBufferCallback callback) {
        database_get_all_buffer(pointer, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getAbove(ByteBuffer key, GetAllBufferCallback callback) {
        database_get_above_buffer(pointer, key.position(), key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBelow(ByteBuffer key, GetAllBufferCallback callback) {
        database_get_below_buffer(pointer, key.position(), key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBetween(ByteBuffer key1, ByteBuffer key2, GetAllBufferCallback callback) {
        database_get_between_buffer(pointer, key1.position(), key1, key2.position(), key2, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public boolean exists(ByteBuffer key) {
        return database_exists_buffer(pointer, key.position(), key);
    }

    public void get(ByteBuffer key, GetKeysBuffersCallback callback) {
        database_get_buffer_with_callback(pointer, key.position(), key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb) ));
    }

    public void get(ByteBuffer key, ByteBuffer value) {
        int valuebytes = database_get_buffer(pointer, key.position(), key, value.capacity(), value);
        value.rewind();
        value.limit(valuebytes);
    }

    public byte[] getCopy(byte[] key) {
        return database_get_bytes(pointer, key);
    }

    public String getCopy(String key) {
        byte[] result = getCopy(key.getBytes());
        return result == null ? null : new String(result);
    }

    public void put(ByteBuffer key, ByteBuffer value) {
        try {
            database_put_buffer(pointer, key.position(), key, value.position(), value);
        } catch (DatabaseException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public boolean remove(ByteBuffer key) {
        try {
            return database_remove_buffer(pointer, key.position(), key);
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
    private native int database_get_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);
    private native void database_get_buffer_with_callback(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native byte[] database_get_bytes(long ptr, byte[] k);
    private native void database_put_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);
    private native boolean database_remove_buffer(long ptr, int kb, ByteBuffer k);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
