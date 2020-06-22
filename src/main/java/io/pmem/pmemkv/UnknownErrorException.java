// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class UnknownErrorException extends DatabaseException {

    public UnknownErrorException(String message) {
        super(message);
    }
}
