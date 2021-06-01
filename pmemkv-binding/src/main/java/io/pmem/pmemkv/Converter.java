// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;

/**
 * Helper interface, which provides functionality of storing objects of any type
 * as ByteBuffer inside the pmemkv datastore.
 *
 * This interface is mostly implemented for {@link io.pmem.pmemkv.Database
 * Database} class to use any, arbitrary type as a key and value.
 *
 * @param <T>
 *            the type of an object, which will be converted to ByteBuffer and
 *            stored in the pmemkv datastore.
 */
public interface Converter<T> {
	/**
	 * Defines how object of type T will be converted to ByteBuffer and stored in
	 * the pmemkv datastore.
	 *
	 * @param entry
	 *            Object of type T, which will be stored in the pmemkv datastore.
	 * @return ByteBuffer representation of passed objects.
	 */
	public ByteBuffer toByteBuffer(T entry);

	/**
	 * Defines how object stored in pmemkv will be converted from ByteBuffer back to
	 * type T to be passed to the callback functions.
	 *
	 * @param entry
	 *            ByteBuffer stored in pmemkv.
	 * @return Object of type T, which will be passed to the callback functions.
	 */
	public T fromByteBuffer(ByteBuffer entry);
}
