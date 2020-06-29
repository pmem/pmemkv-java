// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;


/**
* Argument to function has wrong value
*
* @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>*
*/
public class InvalidArgumentException extends DatabaseException {

    public InvalidArgumentException(String message) {
        super(message);
    }
}
