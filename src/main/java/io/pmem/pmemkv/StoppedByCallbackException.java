// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Callback function aborted
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
public class StoppedByCallbackException extends DatabaseException {

	public StoppedByCallbackException(String message) {
		super(message);
	}
}
