// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;

public interface GetKeysBuffersCallback {

    void process(ByteBuffer key);

}
