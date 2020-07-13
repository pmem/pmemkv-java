// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

/**
 * Engine name does not match any available engine.
 *
 * @see <a href=
 *      "https://pmem.io/pmemkv/master/manpages/libpmemkv.3.html#errors">Pmemkv
 *      errors</a>
 * @see <a href= "https://github.com/pmem/pmemkv#storage-engines">Currently
 *      avaiable pmemkv engines</a>
 */
@SuppressWarnings("serial")
public class WrongEngineNameException extends DatabaseException {

	public WrongEngineNameException(String message) {
		super(message);
	}
}
