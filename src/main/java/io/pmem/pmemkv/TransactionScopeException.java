// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * An error with the scope of the libpmemobj transaction.
 * <p>
 * This exception is defined for compatibility with pmemkv API and most probably
 * never occurs.
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
@SuppressWarnings("serial")
public class TransactionScopeException extends DatabaseException {

	public TransactionScopeException(String message) {
		super(message);
	}
}
