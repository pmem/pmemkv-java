// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
* Operation failed because there is not enough memory (or space on the device)
*
* @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
*/
public class OutOfMemoryException extends DatabaseException {

    public OutOfMemoryException(String message) {
        super(message);
    }
}
