// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
* Config item has different type than expected
*
* @see <a href="https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv errors</a>
*/
public class ConfigTypeErrorException extends DatabaseException {

    public ConfigTypeErrorException(String message) {
        super(message);
    }
}
