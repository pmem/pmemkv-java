// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Pmemkv database configuration failed.
 * <p>
 * It's not an equivalent to any of pmemkv's statuses per se, but it's a close
 * friend to CONFIG_* errors (it varies due to usage of Builder pattern).
 *
 * @see io.pmem.pmemkv.Database.Builder
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 */
@SuppressWarnings("serial")
public class BuilderException extends DatabaseException {

	public BuilderException(String message) {
		super(message);
	}
}
