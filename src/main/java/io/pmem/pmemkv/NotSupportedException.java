// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class NotSupportedException extends DatabaseException {

    public NotSupportedException(String message) {
        super(message);
    }
}
