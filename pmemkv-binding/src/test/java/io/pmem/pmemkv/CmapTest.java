// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

import static io.pmem.pmemkv.TestUtils.*;

public class CmapTest {

	private final String ENGINE = "cmap";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testCreateAndOpen() {
		String file = folder.getRoot() + File.pathSeparator + "testfile";
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, file, new ByteBufferConverter());

		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");

		db.stop();

		db = openDB(ENGINE, file, new ByteBufferConverter());
		assertTrue(db.exists(stringToByteBuffer("key1")));
		resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");
	}

	@Test
	public void throwsExceptionOnStartWhenOpeningNonExistentFile() {
		String file = folder.getRoot() + File.pathSeparator + "testfile";

		Database<ByteBuffer, ByteBuffer> db = null;

		try {
			db = openDB(ENGINE, file, new ByteBufferConverter());
			Assert.fail();
		} catch (DatabaseException e) {
			/* file doesn't exist, open should throw */
		}

		assertNull(db);
	}

	@Test
	public void throwsExceptionOnSortedCountFuncs() {
		String file = folder.getRoot() + File.pathSeparator + "testfile";
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, file, new ByteBufferConverter());
		ByteBuffer key1 = stringToByteBuffer("key1");
		ByteBuffer key2 = stringToByteBuffer("key2");

		try {
			db.countAbove(key1);
			Assert.fail();
		} catch (DatabaseException e) {
			/* countAbove for unsorted engines should throw NotSupported */
		}

		try {
			db.countBelow(key1);
			Assert.fail();
		} catch (DatabaseException e) {
			/* countBelow for unsorted engines should throw NotSupported */
		}

		try {
			db.countBetween(key1, key2);
			Assert.fail();
		} catch (DatabaseException e) {
			/* countBetween for unsorted engines should throw NotSupported */
		}

		db.stop();
	}
}
