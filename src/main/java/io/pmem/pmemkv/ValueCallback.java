// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv;

public interface ValueCallback<ValueT> {

	void process(ValueT value);

}
