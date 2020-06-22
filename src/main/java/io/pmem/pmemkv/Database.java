// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.internal.GetKeysBuffersJNICallback;
import io.pmem.pmemkv.internal.GetAllBufferJNICallback;

import java.nio.ByteBuffer;

public class Database {

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

    public void getKeys(GetKeysByteArraysCallback callback) {
        database_get_keys_bytes(pointer, callback);
    }

    public void getKeys(GetKeysStringsCallback callback) {
        database_get_keys_string(pointer, callback);
    }

    public void getKeysAbove(ByteBuffer key, GetKeysBuffersCallback callback) {
        database_get_keys_above_buffer(pointer, key.position(), key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysAbove(byte[] key, GetKeysByteArraysCallback callback) {
        database_get_keys_above_bytes(pointer, key, callback);
    }

    public void getKeysAbove(String key, GetKeysStringsCallback callback) {
        database_get_keys_above_string(pointer, key.getBytes(), callback);
    }

    public void getKeysBelow(ByteBuffer key, GetKeysBuffersCallback callback) {
        database_get_keys_below_buffer(pointer, key.position(), key, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBelow(byte[] key, GetKeysByteArraysCallback callback) {
        database_get_keys_below_bytes(pointer, key, callback);
    }

    public void getKeysBelow(String key, GetKeysStringsCallback callback) {
        database_get_keys_below_string(pointer, key.getBytes(), callback);
    }

    public void getKeysBetween(ByteBuffer key1, ByteBuffer key2, GetKeysBuffersCallback callback) {
        database_get_keys_between_buffer(pointer, key1.position(), key1, key2.position(), key2, (int kb, ByteBuffer k)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void getKeysBetween(byte[] key1, byte[] key2, GetKeysByteArraysCallback callback) {
        database_get_keys_between_bytes(pointer, key1, key2, callback);
    }

    public void getKeysBetween(String key1, String key2, GetKeysStringsCallback callback) {
        database_get_keys_between_string(pointer, key1.getBytes(), key2.getBytes(), callback);
    }

    public long countAll() {
        return database_count_all(pointer);
    }

    public long countAbove(ByteBuffer key) {
        return database_count_above_buffer(pointer, key.position(), key);
    }

    public long countAbove(byte[] key) {
        return database_count_above_bytes(pointer, key);
    }

    public long countAbove(String key) {
        return database_count_above_bytes(pointer, key.getBytes());
    }

    public long countBelow(ByteBuffer key) {
        return database_count_below_buffer(pointer, key.position(), key);
    }

    public long countBelow(byte[] key) {
        return database_count_below_bytes(pointer, key);
    }

    public long countBelow(String key) {
        return database_count_below_bytes(pointer, key.getBytes());
    }

    public long countBetween(ByteBuffer key1, ByteBuffer key2) {
        return database_count_between_buffer(pointer, key1.position(), key1, key2.position(), key2);
    }

    public long countBetween(byte[] key1, byte[] key2) {
        return database_count_between_bytes(pointer, key1, key2);
    }

    public long countBetween(String key1, String key2) {
        return database_count_between_bytes(pointer, key1.getBytes(), key2.getBytes());
    }

    public void getAll(GetAllBufferCallback callback) {
        database_get_all_buffer(pointer, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getAll(GetAllByteArrayCallback callback) {
        database_get_all_bytes(pointer, callback);
    }

    public void getAll(GetAllStringCallback callback) {
        database_get_all_string(pointer, callback);
    }

    public void getAbove(ByteBuffer key, GetAllBufferCallback callback) {
        database_get_above_buffer(pointer, key.position(), key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getAbove(byte[] key, GetAllByteArrayCallback callback) {
        database_get_above_bytes(pointer, key, callback);
    }

    public void getAbove(String key, GetAllStringCallback callback) {
        database_get_above_string(pointer, key.getBytes(), callback);
    }

    public void getBelow(ByteBuffer key, GetAllBufferCallback callback) {
        database_get_below_buffer(pointer, key.position(), key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBelow(byte[] key, GetAllByteArrayCallback callback) {
        database_get_below_bytes(pointer, key, callback);
    }

    public void getBelow(String key, GetAllStringCallback callback) {
        database_get_below_string(pointer, key.getBytes(), callback);
    }

    public void getBetween(ByteBuffer key1, ByteBuffer key2, GetAllBufferCallback callback) {
        database_get_between_buffer(pointer, key1.position(), key1, key2.position(), key2, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void getBetween(byte[] key1, byte[] key2, GetAllByteArrayCallback callback) {
        database_get_between_bytes(pointer, key1, key2, callback);
    }

    public void getBetween(String key1, String key2, GetAllStringCallback callback) {
        database_get_between_string(pointer, key1.getBytes(), key2.getBytes(), callback);
    }

    public boolean exists(ByteBuffer key) {
        return database_exists_buffer(pointer, key.position(), key);
    }

    public boolean exists(byte[] key) {
        return database_exists_bytes(pointer, key);
    }

    public boolean exists(String key) {
        return database_exists_bytes(pointer, key.getBytes());
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

    public void put(byte[] key, byte[] value) {
        try {
            database_put_bytes(pointer, key, value);
        } catch (DatabaseException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public void put(String key, String value) {
        try {
            database_put_bytes(pointer, key.getBytes(), value.getBytes());
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

    public boolean remove(byte[] key) {
        try {
            return database_remove_bytes(pointer, key);
        } catch (DatabaseException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public boolean remove(String key) {
        try {
            return database_remove_bytes(pointer, key.getBytes());
        } catch (DatabaseException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public static class Builder {
        public Builder(String engine) {
            config = config_new();

            this.engine = engine;
        }

        @Override
        public void finalize() {
            if (config != 0) {
                config_delete(config);
                config = 0;
            }
        }

        public Builder setSize(long size) {
            config_put_int(config, "size", size);
            return this;
        }

        public Builder setForceCreate(boolean forceCreate) {
            config_put_int(config, "size", forceCreate ? 1 : 0);
            return this;
        }

        public Builder setPath(String path) {
            config_put_string(config, "path", path);
            return this;
        }

        public Database build() {
            Database db = new Database(this);

            /* After open, db takes ownership of the config */
            config = 0;

            return db;
        }

        private long config = 0;
        private String engine;

        private native long config_new();
        private native void config_delete(long ptr);
        private native void config_put_int(long ptr, String key, long value);
        private native void config_put_string(long ptr, String key, String value);

        static {
           System.loadLibrary("pmemkv-jni");
        }
    }

    private Database(Builder builder) {
        pointer = database_start(builder.engine, builder.config);
    }

    private final long pointer;
    private boolean stopped;

    // JNI METHODS --------------------------------------------------------------------------------
    private native long database_start(String engine, long config);
    private native void database_stop(long ptr);
    private native void database_get_keys_buffer(long ptr, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_bytes(long ptr, GetKeysByteArraysCallback cb);
    private native void database_get_keys_string(long ptr, GetKeysStringsCallback cb);
    private native void database_get_keys_above_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_above_bytes(long ptr, byte[] k, GetKeysByteArraysCallback cb);
    private native void database_get_keys_above_string(long ptr, byte[] k, GetKeysStringsCallback cb);
    private native void database_get_keys_below_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_below_bytes(long ptr, byte[] k, GetKeysByteArraysCallback cb);
    private native void database_get_keys_below_string(long ptr, byte[] k, GetKeysStringsCallback cb);
    private native void database_get_keys_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2, GetKeysBuffersJNICallback cb);
    private native void database_get_keys_between_bytes(long ptr, byte[] k1, byte[] k2, GetKeysByteArraysCallback cb);
    private native void database_get_keys_between_string(long ptr, byte[] k1, byte[] k2, GetKeysStringsCallback cb);
    private native long database_count_all(long ptr);
    private native long database_count_above_buffer(long ptr, int kb, ByteBuffer k);
    private native long database_count_above_bytes(long ptr, byte[] k);
    private native long database_count_below_buffer(long ptr, int kb, ByteBuffer k);
    private native long database_count_below_bytes(long ptr, byte[] k);
    private native long database_count_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2);
    private native long database_count_between_bytes(long ptr, byte[] k1, byte[] k2);
    private native void database_get_all_buffer(long ptr, GetAllBufferJNICallback cb);
    private native void database_get_all_bytes(long ptr, GetAllByteArrayCallback cb);
    private native void database_get_all_string(long ptr, GetAllStringCallback cb);
    private native void database_get_above_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);
    private native void database_get_above_bytes(long ptr, byte[] k, GetAllByteArrayCallback cb);
    private native void database_get_above_string(long ptr, byte[] k, GetAllStringCallback cb);
    private native void database_get_below_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);
    private native void database_get_below_bytes(long ptr, byte[] k, GetAllByteArrayCallback cb);
    private native void database_get_below_string(long ptr, byte[] k, GetAllStringCallback cb);
    private native void database_get_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2, GetAllBufferJNICallback cb);
    private native void database_get_between_bytes(long ptr, byte[] k1, byte[] k2, GetAllByteArrayCallback cb);
    private native void database_get_between_string(long ptr, byte[] k1, byte[] k2, GetAllStringCallback cb);
    private native boolean database_exists_buffer(long ptr, int kb, ByteBuffer k);
    private native boolean database_exists_bytes(long ptr, byte[] k);
    private native int database_get_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);
    private native byte[] database_get_bytes(long ptr, byte[] k);
    private native void database_put_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);
    private native void database_put_bytes(long ptr, byte[] k, byte[] v);
    private native boolean database_remove_buffer(long ptr, int kb, ByteBuffer k);
    private native boolean database_remove_bytes(long ptr, byte[] k);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
