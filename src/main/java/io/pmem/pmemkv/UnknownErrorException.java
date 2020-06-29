// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Something unexpected happened
 *
 * @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
 */
public class UnknownErrorException extends DatabaseException {

    public UnknownErrorException(String message) {
        super(message);
    }
}
