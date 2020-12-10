// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Argument passed to the function has a wrong value.
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
@SuppressWarnings("serial")
public class InvalidArgumentException extends DatabaseException {

	public InvalidArgumentException(String message) {
		super(message);
	}
}
