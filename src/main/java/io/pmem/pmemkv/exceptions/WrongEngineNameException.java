// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class WrongEngineNameException extends DatabaseException {
    private static final long serialVersionUID = -3004461787196024174L;

    public WrongEngineNameException(String message) {
        super(message);
    }
}