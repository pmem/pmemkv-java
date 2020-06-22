// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class DefragErrorException extends DatabaseException {
    private static final long serialVersionUID = -320140663109993430L;

    public DefragErrorException(String message) {
        super(message);
    }
}