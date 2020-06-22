// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class TransactionScopeErrorException extends DatabaseException {
    private static final long serialVersionUID = -1684146728643733400L;

    public TransactionScopeErrorException(String message) {
        super(message);
    }
}