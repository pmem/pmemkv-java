// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class TransactionScopeErrorException extends DatabaseException {

    public TransactionScopeErrorException(String message) {
        super(message);
    }
}
