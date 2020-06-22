// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class OutOfMemoryException extends DatabaseException {

    public OutOfMemoryException(String message) {
        super(message);
    }
}
