// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class ConfigTypeErrorException extends DatabaseException {
    private static final long serialVersionUID = 2584300608302378713L;

    public ConfigTypeErrorException(String message) {
        super(message);
    }
}