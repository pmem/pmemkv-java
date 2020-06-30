// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.internal.*;

import java.nio.ByteBuffer;

/**
 * Main Java binding pmemkv class, which is a local/embedded key-value datastore
 * optimized for persistent memory. Rather than being tied to a single language
 * or backing implementation, pmemkv provides different options for language
 * bindings and storage engines.
 * <p>
 * This generic class allows to store data of any type (as both key and value).
 * In most cases user needs to implement Converter interface, which provides
 * functionality of converting between key and value types, and ByteBuffer.
 *
 * @see <a href= "https://github.com/pmem/pmemkv/">Pmemkv</a>
 *
 * @param <K>
 *            the type of key stored in pmemkv datastore
 * @param <V>
 *            the type of value stored in pmemkv datastore
 */
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
	 *
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
	 *
	 * @return true if engine is stopped, false if it is running.
	 * @since 1.0
	 */
	public boolean stopped() {
		return stopped;
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore.
	 *
	 * @param callback
	 *            Function to be called for each key.
	 * @since 1.0
	 */
	public void getKeys(KeyCallback<K> callback) {
		database_get_keys_buffer(pointer, (int kb, ByteBuffer k) -> {
			k.rewind().limit(kb);
			K processed_object = keyConverter.fromByteBuffer(k);
			callback.process(processed_object);
		});
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key
	 *            Sets the lower bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @since 1.0
	 */
	public void getKeysAbove(K key, KeyCallback<K> callback) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		database_get_keys_above_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k) -> {
			k.rewind().limit(kb);
			K processed_object = keyConverter.fromByteBuffer(k);
			callback.process(processed_object);
		});
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @since 1.0
	 */
	public void getKeysBelow(K key, KeyCallback<K> callback) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		database_get_keys_below_buffer(pointer, direct_key.position(), direct_key, (int kb, ByteBuffer k) -> {
			k.rewind().limit(kb);
			K processed_object = keyConverter.fromByteBuffer(k);
			callback.process(processed_object);
		});
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @since 1.0
	 */
	public void getKeysBetween(K key1, K key2, KeyCallback<K> callback) {
		ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
		ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
		database_get_keys_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(),
				direct_key2, (int kb, ByteBuffer k) -> {
					k.rewind().limit(kb);
					K processed_object = keyConverter.fromByteBuffer(k);
					callback.process(processed_object);
				});
	}

	/**
	 * Returns number of currently stored key/value pairs in the pmemkv datastore.
	 *
	 * @return Total number of elements in the datastore.
	 * @since 1.0
	 */
	public long countAll() {
		return database_count_all(pointer);
	}

	/**
	 * Returns number of currently stored key/value pairs in the pmemkv datastore,
	 * whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 * 
	 * @param key
	 *            Sets the lower bound for querying.
	 * @return Number of key/value pairs in the datastore, whose keys are greater
	 *         than the given key.
	 * @since 1.0
	 */
	public long countAbove(K key) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		return database_count_above_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Returns number of currently stored key/value pairs in the pmemkv datastore,
	 * whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 * 
	 * @param key
	 *            Sets the upper bound for querying.
	 * @return Number of key/value pairs in the datastore, whose keys are less than
	 *         the given key.
	 * @since 1.0
	 */
	public long countBelow(K key) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		return database_count_below_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Returns number of currently stored key/value pairs in the pmemkv datastore,
	 * whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @return Number of key/value pairs in the datastore, between given keys.
	 * @since 1.0
	 */
	public long countBetween(K key1, K key2) {
		ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
		ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
		return database_count_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(),
				direct_key2);
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore.
	 *
	 * @param callback
	 *            Function to be called for each key/value pair.
	 * @since 1.0
	 */
	public void getAll(KeyValueCallback<K, V> callback) {
		database_get_all_buffer(pointer, (int kb, ByteBuffer k, int vb, ByteBuffer v) -> {
			k.rewind().limit(kb);
			K processed_key = keyConverter.fromByteBuffer(k);
			v.rewind().limit(vb);
			V processed_value = valueConverter.fromByteBuffer(v);
			callback.process(processed_key, processed_value);
		});
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key
	 *            Sets the lower bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 */
	public void getAbove(K key, KeyValueCallback<K, V> callback) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		database_get_above_buffer(pointer, direct_key.position(), direct_key,
				(int kb, ByteBuffer k, int vb, ByteBuffer v) -> {
					k.rewind().limit(kb);
					K processed_key = keyConverter.fromByteBuffer(k);
					v.rewind().limit(vb);
					V processed_value = valueConverter.fromByteBuffer(v);
					callback.process(processed_key, processed_value);
				});

	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 */
	public void getBelow(K key, KeyValueCallback<K, V> callback) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		database_get_below_buffer(pointer, direct_key.position(), direct_key,
				(int kb, ByteBuffer k, int vb, ByteBuffer v) -> {
					k.rewind().limit(kb);
					K processed_key = keyConverter.fromByteBuffer(k);
					v.rewind().limit(vb);
					V processed_value = valueConverter.fromByteBuffer(v);
					callback.process(processed_key, processed_value);
				});
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 */
	public void getBetween(K key1, K key2, KeyValueCallback<K, V> callback) {
		ByteBuffer direct_key1 = getDirectBuffer(keyConverter.toByteBuffer(key1));
		ByteBuffer direct_key2 = getDirectBuffer(keyConverter.toByteBuffer(key2));
		database_get_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2,
				(int kb, ByteBuffer k, int vb, ByteBuffer v) -> {
					k.rewind().limit(kb);
					K processed_key = keyConverter.fromByteBuffer(k);
					v.rewind().limit(vb);
					V processed_value = valueConverter.fromByteBuffer(v);
					callback.process(processed_key, processed_value);
				});
	}

	/**
	 * Verifies the presence of an element with a given key in the pmemkv datastore.
	 *
	 * @param key
	 *            to query for.
	 * @return true if key exists in the datastore, false otherwise
	 */
	public boolean exists(K key) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		return database_exists_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Executes a callback function on the value for a given key
	 *
	 * @param key
	 *            key to query for.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 */
	public void get(K key, ValueCallback<V> callback) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		database_get_buffer_with_callback(pointer, direct_key.position(), direct_key, (int vb, ByteBuffer v) -> {
			v.rewind().limit(vb);
			V processed_object = valueConverter.fromByteBuffer(v);
			callback.process(processed_object);
		});
	}

	/**
	 * Gets copy of value of a given key.
	 *
	 * @param key
	 *            key to query for.
	 * @return Copy of value associated with the given key or null if not found
	 */
	public V getCopy(K key) {
		byte value[];
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		try {
			value = database_get_bytes(pointer, direct_key.position(), direct_key);
		} catch (NotFoundException kve) {
			return null;
		}
		V retval = valueConverter.fromByteBuffer(ByteBuffer.wrap(value));

		return retval;
	}

	/**
	 * Inserts the key/value pair into the pmemkv datastore.
	 *
	 * @param key
	 *            the key
	 * @param value
	 *            data to be inserted for specified key
	 */
	public void put(K key, V value) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		ByteBuffer direct_value = getDirectBuffer(valueConverter.toByteBuffer(value));

		database_put_buffer(pointer, direct_key.position(), direct_key, direct_value.position(), direct_value);
	}

	/**
	 * Removes key/value pair from the pmemkv datastore for given key.
	 *
	 * @param key
	 *            key to query for, to be removed.
	 * @return true if element was removed, false if element didn't exist before
	 *         removal.
	 */
	public boolean remove(K key) {
		ByteBuffer direct_key = getDirectBuffer(keyConverter.toByteBuffer(key));
		return database_remove_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Builder is used to build instances of pmemkv Database class.
	 * <p>
	 * Configuration is composed using setter functions defined in this class.
	 * Pmemkv config fields are mapped to builder setters accordingly.
	 *
	 * @see <a href=
	 *      https://github.com/pmem/pmemkv/blob/master/doc/libpmemkv.7.md#engines>
	 *      Pmemkv engines </a>
	 * @see <a href= https://pmem.io/pmemkv/master/manpages/libpmemkv_config.3.html>
	 *      Pmemkv config </a>
	 */
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

		/**
		 * Sets "size" parameter for pmemkv engine
		 *
		 * @param size
		 *            size of pmemkv datastore
		 * @return this builder object
		 *
		 */
		public Builder<K, V> setSize(long size) {
			config_put_int(config, "size", size);
			return this;
		}

		/**
		 * Sets "force_create" parameter for pmemkv engine
		 *
		 * @param forceCreate
		 *            specify force_create engine parameter
		 * @return this builder object
		 */
		public Builder<K, V> setForceCreate(boolean forceCreate) {
			config_put_int(config, "force_create", forceCreate ? 1 : 0);
			return this;
		}

		/**
		 * Sets path for pmemkv engine
		 *
		 * @param path
		 *            specify path engine parameter
		 * @return this builder
		 */
		public Builder<K, V> setPath(String path) {
			config_put_string(config, "path", path);
			return this;
		}

		/**
		 * Returns an instance of pmemkv Database created from the fields set on this
		 * builder
		 *
		 * @return instance of pmemkv Database
		 */
		public Database<K, V> build() {
			Database<K, V> db = new Database<K, V>(this);

			/* After open, db takes ownership of the config */
			config = 0;

			return db;
		}

		/**
		 * Sets converter object from a given key type K to ByteBuffer.
		 *
		 * All data is internally stored as ByteBuffer. It's possible to store objects
		 * of arbitrary chosen type K as key by providing object, which implements
		 * conversion between K and ByteBuffer. Type of such object has to implement
		 * Converter interface
		 *
		 * @param newKeyConverter
		 *            Converter object from K type to ByteBuffer
		 * @return this builder
		 */
		public Builder<K, V> setKeyConverter(Converter<K> newKeyConverter) {
			this.keyConverter = newKeyConverter;
			return this;
		}

		/**
		 * Sets converter object from a given value type V to ByteBuffer.
		 *
		 * All data is internally stored as ByteBuffer. It's possible to store objects
		 * of arbitrary chosen type V as value by providing object, which implements
		 * conversion between V and ByteBuffer. Type of such object has to implement
		 * Converter interface.
		 *
		 * @param newValueConverter
		 *            Converter object from V type to ByteBuffer
		 *
		 * @return this builder
		 */
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

	// JNI METHODS
	// --------------------------------------------------------------------------------
	private native long database_start(String engine, long config);

	private native void database_stop(long ptr);

	private native void database_get_keys_buffer(long ptr, GetKeysBuffersJNICallback cb);

	private native void database_get_keys_above_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);

	private native void database_get_keys_below_buffer(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);

	private native void database_get_keys_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2,
			GetKeysBuffersJNICallback cb);

	private native long database_count_all(long ptr);

	private native long database_count_above_buffer(long ptr, int kb, ByteBuffer k);

	private native long database_count_below_buffer(long ptr, int kb, ByteBuffer k);

	private native long database_count_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2);

	private native void database_get_all_buffer(long ptr, GetAllBufferJNICallback cb);

	private native void database_get_above_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);

	private native void database_get_below_buffer(long ptr, int kb, ByteBuffer k, GetAllBufferJNICallback cb);

	private native void database_get_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2,
			GetAllBufferJNICallback cb);

	private native boolean database_exists_buffer(long ptr, int kb, ByteBuffer k);

	private native void database_get_buffer_with_callback(long ptr, int kb, ByteBuffer k, GetKeysBuffersJNICallback cb);

	private native byte[] database_get_bytes(long ptr, int kb, ByteBuffer k);

	private native void database_put_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);

	private native boolean database_remove_buffer(long ptr, int kb, ByteBuffer k);

	static {
		System.loadLibrary("pmemkv-jni");
	}
}
