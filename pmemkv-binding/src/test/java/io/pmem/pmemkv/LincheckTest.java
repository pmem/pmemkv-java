// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.jetbrains.kotlinx.lincheck.execution.*;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.verifier.*;
import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.ByteBuffer;

import static io.pmem.pmemkv.TestUtils.*;

@StressCTest(threads = 3, iterations = 10, invocationsPerIteration = 10, requireStateEquivalenceImplCheck = false)
public class LincheckTest {

	private final String ENGINE = "cmap";
	private String DB_PATH = "";
	private Database<ByteBuffer, ByteBuffer> db;

	@Operation(runOnce = true)
	public void put() {
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
	}

	@Operation(runOnce = true)
	public void get() {
		ByteBuffer resBuff;
		try {
			resBuff = db.get(stringToByteBuffer("key1"));
			assertEquals(byteBufferToString(resBuff), "value1");
		} catch (NotFoundException e) {
			System.out.println("just NotFound! ;)");
		}
	}

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_PATH = testDir.getRoot() + File.separator + "testfile";
		assertTrue(DB_PATH != null && !DB_PATH.isEmpty());

		db = createDB(ENGINE, DB_PATH, new ByteBufferConverter());
	}

	@After
	public void finalize() {
		db.stop();
	}

	@Test
	public void testCreateAndOpen() {
		LinChecker.check(LincheckTest.class);
	}
}