// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class DefragErrorException extends DatabaseException {

	public DefragErrorException(String message) {
		super(message);
	}
}
