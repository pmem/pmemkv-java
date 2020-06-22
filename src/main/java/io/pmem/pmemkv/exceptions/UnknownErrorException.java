// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class UnknownErrorException extends DatabaseException {
    private static final long serialVersionUID = -7213171759753486636L;

    public UnknownErrorException(String message) {
        super(message);
    }
}