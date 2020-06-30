// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

@SuppressWarnings("serial")
public class OutOfMemoryException extends DatabaseException {

	public OutOfMemoryException(String message) {
		super(message);
	}
}
