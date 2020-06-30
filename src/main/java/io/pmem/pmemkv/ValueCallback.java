// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Represents callback function, which handle value-only use cases. Such
 * expression may be passed to get*() methods in Database class
 *
 * @param <ValueT>
 *            the type of a key stored in the pmemkv database
 */
@FunctionalInterface
public interface ValueCallback<ValueT> {
	/**
	 * It's internally used as a middle layer to run callback function
	 *
	 * @param value
	 *            the value returned by pmemkv engine to the callback function
	 */
	void process(ValueT value);

}
