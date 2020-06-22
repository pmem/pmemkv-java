// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class OutOfMemoryException extends DatabaseException {
    private static final long serialVersionUID = -6181562207006049434L;

    public OutOfMemoryException(String message) {
        super(message);
    }
}