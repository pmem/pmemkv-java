// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Function is not implemented by the current engine.
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
@SuppressWarnings("serial")
public class NotSupportedException extends DatabaseException {

	public NotSupportedException(String message) {
		super(message);
	}
}
