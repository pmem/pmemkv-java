// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.jetbrains.kotlinx.lincheck.annotations.*;
import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.paramgen.*;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.ByteBuffer;

import static io.pmem.pmemkv.TestUtils.*;

@StressCTest(threads = 3, iterations = 10, invocationsPerIteration = 10, requireStateEquivalenceImplCheck = false)
@Param(name = "key", gen = StringGen.class, conf = "8")
public class LincheckTest {

	private final String ENGINE = "cmap";
	private String DB_PATH = "";
	private Database<ByteBuffer, ByteBuffer> db;

	@Operation(runOnce = true)
	public void put(@Param(name = "key") String k, @Param(name = "key") String v) {
		System.out.println("Visit: " + k + " : " + v);
		ByteBuffer key = stringToByteBuffer(k);
		ByteBuffer value = stringToByteBuffer(v);
		db.put(key, value);
	}

	@Operation(runOnce = true)
	public void put2(@Param(name = "key") String k, @Param(name = "key") String v) {
		System.out.println("Visit: " + k + " : " + v);
		ByteBuffer key = stringToByteBuffer(k);
		ByteBuffer value = stringToByteBuffer(v);
		// ByteBuffer key = stringToByteBuffer("key2");
		// ByteBuffer value = stringToByteBuffer("value2");
		db.put(key, value);
	}

	// @Operation(handleExceptionsAsResult = NotFoundException.class)
	// public void get() {
	// ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
	// if (resBuff != null) {
	// assertEquals(byteBufferToString(resBuff), "value1");
	// }
	// }

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_PATH = testDir.getRoot() + File.separator + "testfile";
		assertTrue(DB_PATH != null && !DB_PATH.isEmpty());

		db = createDB(ENGINE, DB_PATH, new ByteBufferConverter());
		System.out.println("Init DONE !!!");
	}

	@After
	public void finalize() {
		db.stop();
	}

	@Test
	public void basicTest() {
		LinChecker.check(LincheckTest.class);
	}
}