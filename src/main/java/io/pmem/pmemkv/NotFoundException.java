// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class NotFoundException extends DatabaseException {

    public NotFoundException(String message) {
        super(message);
    }
}
