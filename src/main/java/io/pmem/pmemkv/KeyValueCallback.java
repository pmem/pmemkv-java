// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Represents callback function, which handle value-only use cases. Such
 * expression may be passed to get() method in Database class
 *
 * @param <KeyT>
 *            the type of a key stored in the pmemkv database
 * @param <ValueT>
 *            the type of a value stored in the pmemkv database
 */
@FunctionalInterface
public interface KeyValueCallback<KeyT, ValueT> {
	/**
	 * It's internally used as a middle layer to run callback function
	 *
	 * @param key
	 *            the key returned by pmemkv engine to the callback function
	 * @param value
	 *            the value returned by pmemkv engine to the callback function
	 */
	void process(KeyT key, ValueT value);

}
