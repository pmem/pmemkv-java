// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class NotFoundException extends DatabaseException {
    private static final long serialVersionUID = 1435353926864813380L;

    public NotFoundException(String message) {
        super(message);
    }
}