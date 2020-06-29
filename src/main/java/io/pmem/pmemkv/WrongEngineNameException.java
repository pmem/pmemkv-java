// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class WrongEngineNameException extends DatabaseException {

	public WrongEngineNameException(String message) {
		super(message);
	}
}
