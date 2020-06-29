// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.internal.GetKeysBuffersJNICallback;
import io.pmem.pmemkv.internal.GetAllBufferJNICallback;
import io.pmem.pmemkv.DatabaseException;
import io.pmem.pmemkv.Converter;

import java.nio.ByteBuffer;


public class Database<K, V> {
    Converter<K> keyConverter;
    Converter<V> valueConverter;

    private ByteBuffer getDirectBuffer(ByteBuffer buf) {
      if (buf.isDirect()) {
        return buf;
      }
      ByteBuffer directBuffer = ByteBuffer.allocateDirect(buf.capacity());
      directBuffer.put(buf);
      return directBuffer;
    }

   /**
    * Stops the running engine.
    * @since 1.0
    */
    public void stop() {
        if (!stopped) {
            stopped = true;
            database_stop(pointer);
        }
    }

   /**
    * Checks if engine is stopped
    * @return true if engine is stoped, false if is running.
    * @since 1.0
    */
    public boolean stopped() {
        return stopped;
    }

   /**
    * Executes callback function for every key stored in the pmemkv datastore.
    * @param callback Function to be called for each key.
    * @since 1.0
    */
    public void getKeys(KeyCallback<K> callback) {
        database_get_keys_buffer(pointer, (int kb, ByteBuffer k) -> {
          k.rewind().limit(kb);
          K processed_object = (K) keyConverter.fromByteBuffer(k);
          callback.process(processed_object);
        });
    }

   /**
    * Executes callback function for every key stored in the pmemkv datastore, whose keys are greater than the given key.
    * @param key Sets the lower bound for querying.
    * @param callback Function to be called for each key.
    * @since 1.0
    */
    public void getKeysAbove(K key, KeyCallback<K> callback) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        database_get_keys_above_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k)
                -> {
                    k.rewind().limit(kb);
                    K processed_object = (K) keyConverter.fromByteBuffer(k);
                    callback.process(processed_object);
                });
    }

   /**
    * Executes callback function for every key stored in the pmemkv datastore, whose keys are lower than the given key.
    * @param key Sets the upper bound for querying.
    * @param callback Function to be called for each key.
    * @since 1.0
    */
    public void getKeysBelow(K key, KeyCallback<K> callback) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        database_get_keys_below_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k)
                -> {
                    k.rewind().limit(kb);
                    K processed_object = (K) keyConverter.fromByteBuffer(k);
                    callback.process(processed_object);
                });
    }

   /**
    * Executes callback function for every key stored in the pmemkv datastore,
    * whose keys are greater than the key1 and less than the key2.
    * @param key Sets the upper bound for querying.
    * @param callback Function to be called for each key.
    * @since 1.0
    */
    public void getKeysBetween(K key1, K key2, KeyCallback<K> callback) {
        ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
        ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
        database_get_keys_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2, (int kb, ByteBuffer k)
                -> {
                    k.rewind().limit(kb);
                    K processed_object = (K) keyConverter.fromByteBuffer(k);
                    callback.process(processed_object);
                });
    }

   /**
    * Returns number of currently stored key/value pairs in the pmemkv datastore.
    * @return Total number of elements in the datastore.
    * @since 1.0
    */
    public long countAll() {
        return database_count_all(pointer);
    }

   /**
    * Returns number of currently stored key/value pairs in the pmemkv datastore,
    * whose keys are greater than the given key.
    * @param key Sets the lower bound for querying.
    * @return Number of key/value pairs in the datastore, whose keys are greater, than the given key.
    * @since 1.0
    */
    public long countAbove(K key) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        return database_count_above_buffer(pointer, direct_key.position(), direct_key);
    }

   /**
    * Returns number of currently stored key/value pairs in the pmemkv datastore,
    * whose keys are less than the given key.
    * @param key Sets the upper bound for querying.
    * @return Number of key/value pairs in the datastore, whose keys are lower, than the given key.
    * @since 1.0
    */
    public long countBelow(K key) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        return database_count_below_buffer(pointer, direct_key.position(), direct_key);
    }

   /**
    * Returns number of currently stored key/value pairs in the pmemkv datastore,
    * whose keys are greater than the key1 and less than the key2.
    * @param key1 Sets the lower bound for querying.
    * @param key2 Sets the upper bound for querying.
    * @return Number of key/value pairs in the datastore, between given keys.
    * @since 1.0
    */
    public long countBetween(K key1, K key2) {
        ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
        ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
        return database_count_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2);
    }

    /**
     * Executes callback function for every key/value pair stored in the pmemkv  datastore.
     * @param callback Function to be called for each key/value pair.
     * @since 1.0
     */
    public void getAll(KeyValueCallback<K, V>  callback) {
        database_get_all_buffer(pointer, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> {
                    k.rewind().limit(kb);
                    K processed_key = (K) keyConverter.fromByteBuffer(k);
                    v.rewind().limit(vb);
                    V processed_value = (V) valueConverter.fromByteBuffer(v);
                    callback.process(processed_key, processed_value);
                });
    }

    /**
     * Executes callback function for every key/value pair stored in the pmemkv datastore,
     * whose keys are greater than the given key.
     * @param key Sets the lower bound for querying.
     * @param callback Function to be called for each specified key/value pair.
     */
    public void getAbove(K key, KeyValueCallback<K, V>  callback) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        database_get_above_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> {
                    k.rewind().limit(kb);
                    K processed_key = (K) keyConverter.fromByteBuffer(k);
                    v.rewind().limit(vb);
                    V processed_value = (V) valueConverter.fromByteBuffer(v);
                    callback.process(processed_key, processed_value);
                });

    }

    /**
     * Executes callback function for every key/value pair stored in the pmemkv datastore,
     * whose keys are lower than the given key.
     * @param key Sets the upper bound for querying.
     * @param callback Function to be called for each specified key/value pair.
     */
    public void getBelow(K key, KeyValueCallback<K, V>  callback) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        database_get_below_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> {
                    k.rewind().limit(kb);
                    K processed_key = (K) keyConverter.fromByteBuffer(k);
                    v.rewind().limit(vb);
                    V processed_value = (V) valueConverter.fromByteBuffer(v);
                    callback.process(processed_key, processed_value);
                });
    }

    /**
     * Executes callback function for every key/value pair stored in the pmemkv datastore,
     *  whose keys are greater than the key1 and less than the key2.
     * @param key1 Sets the lower bound for querying.
     * @param key1 Sets the upper bound for querying.
     * @param callback Function to be called for each specified key/value pair.
     */
    public void getBetween(K key1, K key2, KeyValueCallback<K, V>  callback) {
        ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
        ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
        database_get_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2, (int kb, ByteBuffer k, int vb, ByteBuffer v)
                -> {
                    k.rewind().limit(kb);
                    K processed_key = (K) keyConverter.fromByteBuffer(k);
                    v.rewind().limit(vb);
                    V processed_value = (V) valueConverter.fromByteBuffer(v);
                    callback.process(processed_key, processed_value);
                });
    }

    /**
     * Verifies the presence key/value pair in the pmemkv datastore.
     * @param key to query for.
     */
    public boolean exists(K key) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        return database_exists_buffer(pointer, direct_key.position(), direct_key);
    }

    /**
     * Executes callback function for value for given key.
     * @param key key to query for.
     * @param callback Function to be called for each specified key/value pair.
     */
    public void get(K key, ValueCallback<V> callback) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        database_get_buffer_with_callback(pointer, direct_key.position(), direct_key, (int vb, ByteBuffer v)
                -> {
                    v.rewind().limit(vb);
                    V processed_object = (V) valueConverter.fromByteBuffer(v);
                    callback.process(processed_object);
                });
    }

    /**
     * Gets copy of value for given key.
     * @param key key to query for.
     * @return Copy of value associated with the given key.
     */
    public V getCopy(K key) {
        byte value[];
         ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        try {
            value = database_get_bytes(pointer, direct_key.position(), direct_key);
        } catch (NotFoundException kve) {
            return null;
        }
        V retval = (V) valueConverter.fromByteBuffer(ByteBuffer.wrap(value));

        return retval;
    }

    /**
     * Inserts the key/value pair into the pmemkv datastore.
     * @param key the key
     * @param value data to be inserted for specified key
     */
    public void put(K key, V value) {
          ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
          ByteBuffer direct_value = getDirectBuffer(valueConverter.toByteBuffer(value));

          database_put_buffer(pointer, direct_key.position(), direct_key, direct_value.position(), direct_value);
    }

    /**
     * Removes key/value pair from the pmemkv datastore for given key.
     * @param key key to query for, to be removed.
     * @return true if element was removed, false if element didn't exist before removal.
     */
    public boolean remove(K key) {
        ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
        return database_remove_buffer(pointer, direct_key.position(), direct_key);
    }

    public static class Builder<K, V> {
        private Converter<K> keyConverter;
        private Converter<V> valueConverter;
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

        public Builder<K, V> setSize(long size) {
            config_put_int(config, "size", size);
            return this;
        }

        public Builder<K, V> setForceCreate(boolean forceCreate) {
            config_put_int(config, "force_create", forceCreate ? 1 : 0);
            return this;
        }

        public Builder<K, V> setPath(String path) {
            config_put_string(config, "path", path);
            return this;
        }
        public Database<K, V> build() {
            Database<K, V>  db = new Database<K, V> (this);

            /* After open, db takes ownership of the config */
            config = 0;

            return db;
        }

        public Builder<K,V> setKeyConverter(Converter<K> newKeyConverter) {
            this.keyConverter = newKeyConverter;
            return this;
        }

        public Builder<K, V> setValueConverter(Converter<V> newValueConverter) {
            this.valueConverter = newValueConverter;
            return this;
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

    private Database(Builder<K, V> builder) {
        keyConverter = builder.keyConverter;
        valueConverter = builder.valueConverter;
        pointer = database_start(builder.engine, builder.config);
    }

    private final long pointer;
    private boolean stopped;

    // JNI METHODS --------------------------------------------------------------------------------
    private native long database_start(String engine, long config);
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
