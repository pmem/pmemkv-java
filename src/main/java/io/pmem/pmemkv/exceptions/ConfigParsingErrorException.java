// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

import io.pmem.pmemkv.exceptions.DatabaseException;

public class ConfigParsingErrorException extends DatabaseException {
    private static final long serialVersionUID = 2323887620705961645L;

    public ConfigParsingErrorException(String message) {
        super(message);
    }
}