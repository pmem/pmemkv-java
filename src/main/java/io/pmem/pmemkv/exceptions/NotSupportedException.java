// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class NotSupportedException extends DatabaseException {
    private static final long serialVersionUID = 780146882310804029L;

    public NotSupportedException(String message) {
        super(message);
    }
}