// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Represents callback function, which handle key-only use cases. Such
 * expression may be passed to getKeys*() methods in Database class.
 *
 * @param <KeyT>
 *            the type of a key stored in the pmemkv database
 */
@FunctionalInterface
public interface KeyCallback<KeyT> {
	/**
	 * It's internally used as a middle layer to run callback function
	 *
	 * @param key
	 *            the key returned by pmemkv engine to callback function
	 */
	void process(KeyT key);

}
