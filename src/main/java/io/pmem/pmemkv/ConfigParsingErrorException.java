// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.DatabaseException;

public class ConfigParsingErrorException extends DatabaseException {

	public ConfigParsingErrorException(String message) {
		super(message);
	}
}
