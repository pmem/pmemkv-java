// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class UnknownErrorException extends DatabaseException {

    public UnknownErrorException(String message) {
        super(message);
    }
}
