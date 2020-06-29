// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.DatabaseException;

/**
 * Processing config failed
 * 
 * @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
 *
 */
public class ConfigParsingErrorException extends DatabaseException {

    public ConfigParsingErrorException(String message) {
        super(message);
    }
}
