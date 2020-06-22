// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv.exceptions;

public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    private Object key;

}
