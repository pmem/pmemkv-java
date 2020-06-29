// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Database entry or config item not found
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
public class NotFoundException extends DatabaseException {

	public NotFoundException(String message) {
		super(message);
	}
}
