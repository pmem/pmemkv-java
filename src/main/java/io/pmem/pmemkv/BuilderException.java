// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

@SuppressWarnings("serial")
public class BuilderException extends DatabaseException {

	public BuilderException(String message) {
		super(message);
	}
}
