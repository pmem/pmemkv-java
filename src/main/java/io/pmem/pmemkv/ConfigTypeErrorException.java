// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

public class ConfigTypeErrorException extends DatabaseException {

    public ConfigTypeErrorException(String message) {
        super(message);
    }
}
