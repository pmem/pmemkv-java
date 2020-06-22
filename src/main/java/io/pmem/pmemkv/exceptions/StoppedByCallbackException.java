// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class StoppedByCallbackException extends DatabaseException {
    private static final long serialVersionUID = -6355814420128328221L;

    public StoppedByCallbackException(String message) {
        super(message);
    }
}