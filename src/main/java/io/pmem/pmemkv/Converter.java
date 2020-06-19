// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;

public interface Converter<T> {
    public ByteBuffer toByteBuffer(T entry);
    public T fromByteBuffer(ByteBuffer entry);
}

