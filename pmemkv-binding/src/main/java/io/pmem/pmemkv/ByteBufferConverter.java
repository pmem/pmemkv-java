// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2022, Intel Corporation */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;

/**
 * Implementation of Converter interface for ByteBuffer type
 */
public class ByteBufferConverter implements Converter<ByteBuffer> {
	public ByteBuffer toByteBuffer(ByteBuffer entry) {
		return entry;
	}

	public ByteBuffer fromByteBuffer(ByteBuffer entry) {
		return entry;
	}
}
