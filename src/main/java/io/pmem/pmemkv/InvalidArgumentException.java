// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class InvalidArgumentException extends DatabaseException {

	public InvalidArgumentException(String message) {
		super(message);
	}
}
