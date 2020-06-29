// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

/**
* DatabaseException is the superclass of those exceptions that can be thrown during
* of handling pmemkv errors
*
* @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
*/

public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }
}
