// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

package io.pmem.pmemkv;

import java.io.*;
import java.lang.IllegalArgumentException;
import java.lang.OutOfMemoryError;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Main Java binding pmemkv class, which is a local/embedded key-value datastore
 * optimized for persistent memory. Rather than being tied to a single language
 * or backing implementation, pmemkv provides different options for language
 * bindings and storage engines.
 * <p>
 * This generic class allows to store data of any type (as both key and value).
 * In most cases user needs to implement {@link io.pmem.pmemkv.Converter
 * Converter} interface, which provides functionality of converting between key
 * and value types, and ByteBuffer.
 *
 * @see <a href= "https://github.com/pmem/pmemkv/">Pmemkv library
 *      description</a>
 *
 * @param <K>
 *            the type of key stored in the pmemkv datastore
 * @param <V>
 *            the type of value stored in the pmemkv datastore
 */
public class Database<K, V> {
	Converter<K> keyConverter;
	Converter<V> valueConverter;
	private int keyBufferSize;
	private int valueBufferSize;

	/**
	 * Binding for a pmemkv iterator API. Iterator provides methods to iterate over
	 * records in db. This object can be created only using Database method:
	 * iterator().
	 * <p>
	 * Important: This is an experimental feature and should not be used in
	 * production code. For now, we don't guarantee stability of this API.
	 */
	public class WriteIterator implements AutoCloseable {
		/**
		 * Constructor for iterator class. Can be accessed only via Database API.
		 *
		 * @param database_handle
		 *            handle to database pointer
		 */
		WriteIterator(long database_handle) {
			db_ptr = database_handle;
			it_ptr = iterator_new_write_iterator(db_ptr);
		}

		/**
		 * Changes iterator position to the first record.
		 *
		 * @return true if success, false otherwise
		 */
		public boolean seekToFirst() {
			return iterator_seek_to_first(it_ptr);
		}

		/**
		 * Returns record's key.
		 *
		 * If the iterator is on an undefined position, calling this method is undefined
		 * behaviour.
		 *
		 * @return key of type K
		 */
		public K key() {
			ByteBuffer value;
			try {
				value = iterator_key(it_ptr);
			} catch (NotFoundException kve) {
				return null;
			}
			K retval = keyConverter.fromByteBuffer(value);
			return retval;
		}

		/**
		 * Checks if there is a next record available.
		 *
		 * If true is returned, it is guaranteed that iterator.next() will return
		 * status::OK, otherwise iterator is already on the last element and
		 * iterator.next() will return false.
		 *
		 * @return true if there is a next record available, false otherwise.
		 */
		public boolean isNext() {
			return iterator_is_next(it_ptr);
		}

		/**
		 * Changes iterator position to the next record.
		 *
		 * If the next record exists, returns true, otherwise false is returned and the
		 * iterator position is undefined.
		 *
		 * @return true if the iterator was moved on the next record, false otherwise.
		 */
		public boolean next() {
			return iterator_next(it_ptr);
		}

		/**
		 * Releases underlying resources
		 */
		public void close() {
			iterator_close(it_ptr);
			it_ptr = 0;
		}

		private native long iterator_new_write_iterator(long database_handle);
		private native void iterator_seek(long iterator_handle, String key);
		private native void iterator_seek_lower(long iterator_handle, String key);
		private native void iterator_seek_lower_eq(long iterator_handle, String key);
		private native void iterator_seek_higher(long iterator_handle, String key);
		private native void iterator_seek_higher_eq(long iterator_handle, String key);
		private native boolean iterator_seek_to_first(long iterator_handle);
		private native void iterator_seek_to_last(long iterator_handle);
		private native boolean iterator_is_next(long iterator_handle);
		private native boolean iterator_next(long iterator_handle);
		private native void iterator_prev(long iterator_handle);
		private native ByteBuffer iterator_key(long iterator_handle);
		private native byte[] iterator_read_range(long iterator_handle);
		// write_range()
		private native void iterator_commit(long iterator_handle);
		private native void iterator_abort(long iterator_handle);
		private native void iterator_close(long iterator_handle);

		private long it_ptr;
		private final long db_ptr;
	}

	private class ThreadDirectBuffers {
		public final static int KEY1_BUFFER = 0;
		public final static int KEY2_BUFFER = 1;
		public final static int VALUE_BUFFER = 2;

		private final ArrayList<ByteBuffer> buffers = new ArrayList<>(3);

		public ThreadDirectBuffers(int keySize, int valueSize) {
			buffers.add(ByteBuffer.allocateDirect(keySize));
			buffers.add(ByteBuffer.allocateDirect(keySize));
			buffers.add(ByteBuffer.allocateDirect(valueSize));
		}

		public ByteBuffer get(int number) {
			return buffers.get(number);
		}
	}

	/*
	 * Every thread has its own preallocated direct ByteBuffers which are used
	 * during most operations. Without that optimization, it would be necessary to
	 * allocate a new direct ByteBuffers (that is a very costly operation) in case
	 * of almost every operation. Sizes of this cached buffers can be set during
	 * creating DB (setKeyBufferSize(int) and setValueBufferSize(int)).
	 */
	private ThreadLocal<ThreadDirectBuffers> directBuffers = new ThreadLocal<ThreadDirectBuffers>() {
		@Override
		public ThreadDirectBuffers initialValue() {
			return new ThreadDirectBuffers(keyBufferSize, valueBufferSize);
		}
	};

	private ByteBuffer getDirectBuffer(ByteBuffer buf, int number) {
		if (buf.isDirect()) {
			return buf;
		}
		ByteBuffer directBuffer = directBuffers.get().get(number);
		directBuffer.position(0);
		try {
			directBuffer.put(buf);
		} catch (BufferOverflowException e) {
			directBuffer = ByteBuffer.allocateDirect(buf.capacity());
			directBuffer.put(buf);
		}
		return directBuffer;
	}

	private ByteBuffer getDirectKeyBuffer(ByteBuffer buf, int number) {
		assert number == ThreadDirectBuffers.KEY1_BUFFER || number == ThreadDirectBuffers.KEY2_BUFFER;
		return getDirectBuffer(buf, number);
	}

	private ByteBuffer getDirectKeyBuffer(ByteBuffer buf) {
		return getDirectBuffer(buf, ThreadDirectBuffers.KEY1_BUFFER);
	}

	private ByteBuffer getDirectValueBuffer(ByteBuffer buf) {
		return getDirectBuffer(buf, ThreadDirectBuffers.VALUE_BUFFER);
	}

	/*
	 * These callback wrappers optimize invoking callbacks from the JNI layer. If we
	 * want to call a java method from the JNI, we have to know its ID. Getting this
	 * ID for every callback is a costly operation so with these static wrappers we
	 * only need to get ID once and later we only call these static methods with a
	 * real callback as a parameter which will be invoked inside of these methods.
	 */
	private static <Key, Value> void valueCallbackWrapper(Database<Key, Value> db, ValueCallback<Value> callback, int s,
			ByteBuffer b) {
		b.rewind().limit(s);
		callback.process(db.valueConverter.fromByteBuffer(b));
	}

	private static <Key, Value> void keyCallbackWrapper(Database<Key, Value> db, KeyCallback<Key> callback, int s,
			ByteBuffer b) {
		b.rewind().limit(s);
		callback.process(db.keyConverter.fromByteBuffer(b));
	}

	private static <Key, Value> void keyValueCallbackWrapper(Database<Key, Value> db,
			KeyValueCallback<Key, Value> callback, int kb, ByteBuffer k, int vb, ByteBuffer v) {
		k.rewind().limit(kb);
		Key processed_key = db.keyConverter.fromByteBuffer(k);
		v.rewind().limit(vb);
		Value processed_value = db.valueConverter.fromByteBuffer(v);
		callback.process(processed_key, processed_value);
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
	 * Checks if engine is stopped.
	 *
	 * @return true if engine is stopped, false if it is running.
	 * @since 1.0
	 */
	public boolean stopped() {
		return stopped;
	}

	/**
	 * Iterator provides methods to iterate over records in db. Using iterator() is
	 * the only one way to get an iterator object.
	 *
	 * @return XXX
	 * @throws DatabaseException
	 *             XXX
	 */
	public WriteIterator iterator() throws DatabaseException {
		return new WriteIterator(pointer);
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore. Any
	 * exception thrown by the user from callback will be propagated.
	 *
	 * @param callback
	 *            Function to be called for each key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @throws OutOfMemoryError
	 *             Exception will be thrown when data cannot be allocated in DRAM.
	 * @since 1.0
	 */
	public void getKeys(KeyCallback<K> callback) throws DatabaseException, OutOfMemoryError {
		database_get_keys_buffer(pointer, callback);
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++. Any exception thrown by the user
	 * from callback will be propagated.
	 *
	 * @param key
	 *            Sets the lower bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @throws OutOfMemoryError
	 *             Exception will be thrown when data cannot be allocated in DRAM.
	 * @since 1.0
	 */
	public void getKeysAbove(K key, KeyCallback<K> callback) throws DatabaseException, OutOfMemoryError {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		database_get_keys_above_buffer(pointer, direct_key.position(), direct_key, callback);
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++. Any exception thrown by the user
	 * from callback will be propagated.
	 *
	 * @param key
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @throws OutOfMemoryError
	 *             Exception will be thrown when data cannot be allocated in DRAM.
	 * @since 1.0
	 */
	public void getKeysBelow(K key, KeyCallback<K> callback) throws DatabaseException, OutOfMemoryError {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		database_get_keys_below_buffer(pointer, direct_key.position(), direct_key, callback);
	}

	/**
	 * Executes callback function for every key stored in the pmemkv datastore,
	 * whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++. Any exception thrown by the user
	 * from callback will be propagated.
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @throws OutOfMemoryError
	 *             Exception will be thrown when data cannot be allocated in DRAM.
	 * @since 1.0
	 */
	public void getKeysBetween(K key1, K key2, KeyCallback<K> callback)
			throws DatabaseException, OutOfMemoryError {
		ByteBuffer direct_key1 = getDirectKeyBuffer(keyConverter.toByteBuffer(key1), ThreadDirectBuffers.KEY1_BUFFER);
		ByteBuffer direct_key2 = getDirectKeyBuffer(keyConverter.toByteBuffer(key2), ThreadDirectBuffers.KEY2_BUFFER);
		database_get_keys_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(),
				direct_key2, callback);
	}

	/**
	 * Returns number of key/value pairs currently stored in the pmemkv datastore.
	 *
	 * @return Total number of elements in the datastore.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public long countAll() throws DatabaseException {
		return database_count_all(pointer);
	}

	/**
	 * Returns number of key/value pairs currently stored in the pmemkv datastore,
	 * whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key
	 *            Sets the lower bound for querying.
	 * @return Number of key/value pairs in the datastore, whose keys are greater
	 *         than the given key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public long countAbove(K key) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		return database_count_above_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Returns number of key/value pairs currently stored in the pmemkv datastore,
	 * whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key
	 *            Sets the upper bound for querying.
	 * @return Number of key/value pairs in the datastore, whose keys are less than
	 *         the given key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public long countBelow(K key) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		return database_count_below_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Returns number of key/value pairs currently stored in the pmemkv datastore,
	 * whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @return Number of key/value pairs in the datastore, between given keys.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public long countBetween(K key1, K key2) throws DatabaseException {
		ByteBuffer direct_key1 = getDirectKeyBuffer(keyConverter.toByteBuffer(key1), ThreadDirectBuffers.KEY1_BUFFER);
		ByteBuffer direct_key2 = getDirectKeyBuffer(keyConverter.toByteBuffer(key2), ThreadDirectBuffers.KEY2_BUFFER);
		return database_count_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(),
				direct_key2);
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore. Any exception thrown by the user from callback will be propagated.
	 *
	 * @param callback
	 *            Function to be called for each key/value pair.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public void getAll(KeyValueCallback<K, V> callback) throws DatabaseException {
		database_get_all_buffer(pointer, callback);
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are greater than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key
	 *            Sets the lower bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public void getAbove(K key, KeyValueCallback<K, V> callback) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		database_get_above_buffer(pointer, direct_key.position(), direct_key, callback);
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are less than the given key.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public void getBelow(K key, KeyValueCallback<K, V> callback) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		database_get_below_buffer(pointer, direct_key.position(), direct_key, callback);
	}

	/**
	 * Executes callback function for every key/value pair stored in the pmemkv
	 * datastore, whose keys are greater than the key1 and less than the key2.
	 * <p>
	 * Comparison mechanism is based on binary comparison of bytes - by a function
	 * equivalent to std::string::compare in C++.
	 *
	 * @param key1
	 *            Sets the lower bound for querying.
	 * @param key2
	 *            Sets the upper bound for querying.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public void getBetween(K key1, K key2, KeyValueCallback<K, V> callback) throws DatabaseException {
		ByteBuffer direct_key1 = getDirectKeyBuffer(keyConverter.toByteBuffer(key1), ThreadDirectBuffers.KEY1_BUFFER);
		ByteBuffer direct_key2 = getDirectKeyBuffer(keyConverter.toByteBuffer(key2), ThreadDirectBuffers.KEY2_BUFFER);
		database_get_between_buffer(pointer, direct_key1.position(), direct_key1, direct_key2.position(), direct_key2,
				callback);
	}

	/**
	 * Verifies the presence of an element with a given key in the pmemkv datastore.
	 *
	 * @param key
	 *            key to query for.
	 * @return true if key exists in the datastore, false otherwise
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public boolean exists(K key) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		return database_exists_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Executes callback function on the value for a given key. It allows to read
	 * the entire value for a given key. Any exception thrown by the user from
	 * callback will be propagated.
	 *
	 * @param key
	 *            key to query for.
	 * @param callback
	 *            Function to be called for each specified key/value pair.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @throws OutOfMemoryError
	 *             Exception will be thrown when data cannot be allocated in DRAM.
	 * @since 1.0
	 */
	public void get(K key, ValueCallback<V> callback) throws DatabaseException, OutOfMemoryError {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		database_get_buffer_with_callback(pointer, direct_key.position(), direct_key, callback);
	}

	/**
	 * Gets a copy of the entire value for a given key.
	 *
	 * @param key
	 *            key to query for.
	 * @return Copy of the entire value associated with the given key, or null if
	 *         not found.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public V getCopy(K key) throws DatabaseException {
		byte value[];
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		try {
			value = database_get_bytes(pointer, direct_key.position(), direct_key);
		} catch (NotFoundException kve) {
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new DatabaseException("Internal exception occured.");
		}
		V retval = valueConverter.fromByteBuffer(ByteBuffer.wrap(value));

		return retval;
	}

	/**
	 * Inserts new key/value pair into the pmemkv datastore. If the record with
	 * selected key already exists it will replace the entire (existing) value with
	 * new value.
	 *
	 * @param key
	 *            the key.
	 * @param value
	 *            data to be inserted for the specified key.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public void put(K key, V value) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		ByteBuffer direct_value = getDirectValueBuffer(valueConverter.toByteBuffer(value));

		database_put_buffer(pointer, direct_key.position(), direct_key, direct_value.position(), direct_value);
	}

	/**
	 * Removes key/value pair from the pmemkv datastore for given key.
	 *
	 * @param key
	 *            key to query for, to be removed.
	 * @return true if element was removed, false if element didn't exist before
	 *         removal.
	 * @throws DatabaseException
	 *             or derived class that matches pmemkv's status.
	 * @since 1.0
	 */
	public boolean remove(K key) throws DatabaseException {
		ByteBuffer direct_key = getDirectKeyBuffer(keyConverter.toByteBuffer(key));
		return database_remove_buffer(pointer, direct_key.position(), direct_key);
	}

	/**
	 * Builder is used to build instances of pmemkv Database class.
	 * <p>
	 * Configuration is composed using setter functions defined in this class.
	 * Pmemkv config fields are mapped to builder setters accordingly. Each engine
	 * may require various config parameters.
	 *
	 * @see <a href=
	 *      https://github.com/pmem/pmemkv/blob/master/doc/libpmemkv.7.md#engines>
	 *      Pmemkv engines and their configuration settings description</a>
	 * @see <a href= https://pmem.io/pmemkv/master/manpages/libpmemkv_config.3.html>
	 *      manpage of pmemkv config class</a>
	 */
	public static class Builder<K, V> {
		private Converter<K> keyConverter;
		private Converter<V> valueConverter;
		private int keyBufferSize = 10485760; /* default size - 10MiB */
		private int valueBufferSize = 10485760;

		public Builder(String engine) {
			config = config_new();
			this.engine = engine;
		}

		/**
		 * Frees underlying resources.
		 */
		@Override
		public void finalize() throws Throwable {
			try {
				if (config != 0) {
					config_delete(config);
					config = 0;
				}
			} finally {
				super.finalize();
			}
		}

		/**
		 * Sets "size" parameter for pmemkv engine.
		 *
		 * @param size
		 *            size of the pmemkv datastore.
		 * @return this builder object.
		 * @throws BuilderException
		 *             with pmemkv's config return status.
		 * @since 1.0
		 */
		public Builder<K, V> setSize(long size) throws BuilderException {
			config_put_int(config, "size", size);
			return this;
		}

		/**
		 * Sets "force_create" parameter for pmemkv engine.
		 *
		 * @param forceCreate
		 *            specify force_create engine's parameter.
		 * @return this builder object.
		 * @throws BuilderException
		 *             with pmemkv's config return status.
		 * @since 1.0
		 */
		public Builder<K, V> setForceCreate(boolean forceCreate) throws BuilderException {
			config_put_int(config, "force_create", forceCreate ? 1 : 0);
			return this;
		}

		/**
		 * Sets path for pmemkv engine
		 *
		 * @param path
		 *            specify path engine's parameter.
		 * @return this builder object.
		 * @throws BuilderException
		 *             with pmemkv's config return status.
		 * @since 1.0
		 */
		public Builder<K, V> setPath(String path) throws BuilderException {
			config_put_string(config, "path", path);
			return this;
		}

		/**
		 * Reads config parameters from JSON Object stored in a string. One by one, each
		 * parameter is inserted into config. It can be used alternatively with
		 * setPath/setSize/etc., but it will fail if defining the same parameter twice.
		 *
		 * @param json
		 *            config parameters, given as proper JSON Object.
		 * @return this builder object.
		 * @throws BuilderException
		 *             with pmemkv's config return status.
		 * @since 1.2.0
		 */
		public Builder<K, V> fromJson(String json) throws BuilderException {
			config_from_json(config, json);
			return this;
		}

		/**
		 * Reads config parameters from JSON Object, stored in an InputStream. It
		 * behaves exactly as {@link #fromJson(String) fromJson(String)}.
		 *
		 * @param jsonStream
		 *            config parameters, given as InputStream (encoded in UTF_8) with
		 *            proper JSON Object.
		 * @return this builder object.
		 * @throws BuilderException
		 *             with pmemkv's config return status.
		 * @since 1.2.0
		 */
		public Builder<K, V> fromJson(InputStream jsonStream) throws BuilderException {
			String jsonContent = null;
			try (Scanner scanner = new Scanner(jsonStream, UTF_8.name())) {
				/*
				 * sets delimiter to the beginning of the input, so it reads the whole content
				 */
				jsonContent = scanner.useDelimiter("\\A").next();
			}
			return fromJson(jsonContent);
		}

		/**
		 * Returns an instance of pmemkv Database created from the config parameters set
		 * within this builder.
		 *
		 * @return instance of pmemkv Database.
		 * @since 1.0
		 */
		public Database<K, V> build() {
			/* Engine takes config's ownership */
			return new Database<K, V>(this);
		}

		/**
		 * Sets converter object from a given key type K to ByteBuffer.
		 * <p>
		 * All data is internally stored as ByteBuffer. It's possible to store objects
		 * of an arbitrary chosen type K as a key by providing object, which implements
		 * conversion between K and ByteBuffer. Type of such object has to implement
		 * Converter interface.
		 *
		 * @param newKeyConverter
		 *            Converter object from K type to ByteBuffer.
		 * @return this builder object.
		 * @since 1.0
		 */
		public Builder<K, V> setKeyConverter(Converter<K> newKeyConverter) {
			this.keyConverter = newKeyConverter;
			return this;
		}

		/**
		 * Sets converter object from a given value type V to ByteBuffer.
		 * <p>
		 * All data is internally stored as ByteBuffer. It's possible to store objects
		 * of an arbitrary chosen type V as a value by providing object, which
		 * implements conversion between V and ByteBuffer. Type of such object has to
		 * implement Converter interface.
		 *
		 * @param newValueConverter
		 *            Converter object from V type to ByteBuffer.
		 *
		 * @return this builder object.
		 * @since 1.0
		 */
		public Builder<K, V> setValueConverter(Converter<V> newValueConverter) {
			this.valueConverter = newValueConverter;
			return this;
		}

		/**
		 * Sets a size of a preallocated direct buffers for keys (there are 2 buffers
		 * per thread). These buffers allow avoiding further costly allocations. So the
		 * size should be bigger than most of the expected keys in the DB, because in
		 * case of every bigger key than the size of the buffer, an additional
		 * allocation is required. Every thread has its own two buffers so the real size
		 * of used memory will be 2 * size * numberOfThreads. The default size is 10MiB,
		 * max size is Integer.MAX_VALUE bytes.
		 *
		 * @param size
		 *            Size of a key buffer(s) in bytes.
		 *
		 * @return this builder object.
		 * @throws IllegalArgumentException
		 *             if size {@literal <}= 0
		 * @since 1.1.0
		 */
		public Builder<K, V> setKeyBufferSize(int size) throws IllegalArgumentException {
			if (size <= 0)
				throw new IllegalArgumentException("Buffer size must be > 0");

			this.keyBufferSize = size;
			return this;
		}

		/**
		 * Sets a size of a preallocated direct buffer for values. This buffer allows
		 * avoiding further costly allocations. So the size should be bigger than most
		 * of the expected values in the DB, because in case of every bigger value than
		 * the size of the buffer, an additional allocation is required. Every thread
		 * has its own buffer so the real size of used memory will be size *
		 * numberOfThreads. The default size is 10MiB, max size is Integer.MAX_VALUE
		 * bytes.
		 *
		 * @param size
		 *            Size of a value buffer in bytes.
		 *
		 * @return this builder object.
		 * @throws IllegalArgumentException
		 *             if size {@literal <}= 0
		 * @since 1.1.0
		 */
		public Builder<K, V> setValueBufferSize(int size) throws IllegalArgumentException {
			if (size <= 0)
				throw new IllegalArgumentException("Buffer size must be > 0");

			this.valueBufferSize = size;
			return this;
		}

		// JNI DATABASE BUILDER METHODS
		// --------------------------------------------------------------------------------
		private long config = 0;
		private String engine;

		private native long config_new();

		private native void config_delete(long ptr);

		private native void config_put_int(long ptr, String key, long value);

		private native void config_put_string(long ptr, String key, String value);

		private native void config_from_json(long ptr, String json);

		static {
			boolean unsatisfied_error = false;
			try {
				System.loadLibrary("pmemkv-jni");
			} catch (UnsatisfiedLinkError e) {
				unsatisfied_error = true;
			}
			if (unsatisfied_error) {
				InputStream is = Database.class.getResourceAsStream("/libpmemkv-jni.so.1");

				File file;
				try {
					if (is == null) {
						throw new Exception("Cannot open stream and get resource from Jar file.");
					}
					file = File.createTempFile("lib", ".so");
					OutputStream os = null;
					os = new FileOutputStream(file);
					byte[] buf = new byte[8192];
					int length;
					while ((length = is.read(buf)) > 0) {
						os.write(buf, 0, length);
					}
					is.close();
					os.close();

					System.load(file.getAbsolutePath());
					file.deleteOnExit();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}

	private Database(Builder<K, V> builder) {
		keyConverter = builder.keyConverter;
		valueConverter = builder.valueConverter;
		keyBufferSize = builder.keyBufferSize;
		valueBufferSize = builder.valueBufferSize;
		long config = builder.config;
		builder.config = 0;
		pointer = database_start(builder.engine, config);
	}

	private final long pointer;
	private boolean stopped;

	// JNI DATABASE METHODS
	// --------------------------------------------------------------------------------
	private native long database_start(String engine, long config);

	private native void database_stop(long ptr);

	private native void database_get_keys_buffer(long ptr, KeyCallback<K> cb);

	private native void database_get_keys_above_buffer(long ptr, int kb, ByteBuffer k, KeyCallback<K> cb);

	private native void database_get_keys_below_buffer(long ptr, int kb, ByteBuffer k, KeyCallback<K> cb);

	private native void database_get_keys_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2,
			KeyCallback<K> cb);

	private native long database_count_all(long ptr);

	private native long database_count_above_buffer(long ptr, int kb, ByteBuffer k);

	private native long database_count_below_buffer(long ptr, int kb, ByteBuffer k);

	private native long database_count_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2);

	private native void database_get_all_buffer(long ptr, KeyValueCallback<K, V> cb);

	private native void database_get_above_buffer(long ptr, int kb, ByteBuffer k, KeyValueCallback<K, V> cb);

	private native void database_get_below_buffer(long ptr, int kb, ByteBuffer k, KeyValueCallback<K, V> cb);

	private native void database_get_between_buffer(long ptr, int kb1, ByteBuffer k1, int kb2, ByteBuffer k2,
			KeyValueCallback<K, V> cb);

	private native boolean database_exists_buffer(long ptr, int kb, ByteBuffer k);

	private native void database_get_buffer_with_callback(long ptr, int kb, ByteBuffer k, ValueCallback<V> cb);

	private native byte[] database_get_bytes(long ptr, int kb, ByteBuffer k);

	private native void database_put_buffer(long ptr, int kb, ByteBuffer k, int vb, ByteBuffer v);

	private native boolean database_remove_buffer(long ptr, int kb, ByteBuffer k);
}
