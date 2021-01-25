// SPDX-License-Identifier: BSD-3-Clause
// Copyright 2017-2020, Intel Corporation

package io.pmem.pmemkv.internal;

import java.nio.ByteBuffer;

public interface GetKeysBuffersJNICallback {
	void process(int keybytes, ByteBuffer key);
}
