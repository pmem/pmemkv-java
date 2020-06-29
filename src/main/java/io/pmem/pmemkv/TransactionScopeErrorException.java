// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * An error with the scope of the libpmemobj transaction
 *
 * This exception is defined for compatibility with pmemkv API and probably will never occur
 *
 * @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
 */
public class TransactionScopeErrorException extends DatabaseException {

    public TransactionScopeErrorException(String message) {
        super(message);
    }
}
