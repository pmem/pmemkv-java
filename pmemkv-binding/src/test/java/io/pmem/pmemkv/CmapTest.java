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

	/* Test using multiple instances of the one DB */
	@Test
	public void multipleDBInstancesTest() {
		final int threadsNumber = 8;
		final int numberOfElements = 50;

		String file = folder.getRoot() + File.pathSeparator + "testfile";
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, file, new ByteBufferConverter());
		db.stop();

		runParallel(threadsNumber, () -> {
			Database<ByteBuffer, ByteBuffer> threadDB = openDB(ENGINE, file, new ByteBufferConverter());
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				threadDB.put(stringToByteBuffer(Integer.toString(x)),
						stringToByteBuffer(Integer.toString(x + 1)));
			}
			threadDB.stop();
		});

		runParallel(threadsNumber, () -> {
			Database<ByteBuffer, ByteBuffer> threadDB = openDB(ENGINE, file, new ByteBufferConverter());
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				threadDB.exists(stringToByteBuffer(Integer.toString(x)));
			}
			threadDB.stop();
		});

		runParallel(threadsNumber, () -> {
			Database<ByteBuffer, ByteBuffer> threadDB = openDB(ENGINE, file, new ByteBufferConverter());
			for (int j = numberOfElements - 1; j >= 0; --j) {
				final int x = j;
				threadDB.get(stringToByteBuffer(Integer.toString(x)), (ByteBuffer v) -> {
					assertEquals(byteBufferToString(v), Integer.toString(x + 1));
				});
			}
			threadDB.stop();
		});
	}

	/* Test using multiple instances of 2 different DBs */
	@Test
	public void multipleDBTypesTest() {
		final int threadsNumber = 4;
		final int numberOfElements = 50;

		String file1 = folder.getRoot() + File.pathSeparator + "testfile1";
		Database<String, String> dbString = createDB(ENGINE, file1, new StringConverter(), 10, 100);
		final StringBuilder sb = new StringBuilder(numberOfElements + 1);
		sb.append('x');
		for (int i = 0; i < numberOfElements; ++i) {
			sb.append('x');
			dbString.put(sb.substring(0, i), sb.substring(0, i + 1));
		}
		dbString.stop();

		String file2 = folder.getRoot() + File.pathSeparator + "testfile2";
		Database<ByteBuffer, ByteBuffer> dbByteBuffer = createDB(ENGINE, file2, new ByteBufferConverter(), 20, 200);
		for (int i = 0; i < numberOfElements; ++i) {
			dbByteBuffer.put(stringToByteBuffer(sb.substring(0, i)), stringToByteBuffer(sb.substring(0, i + 1)));
		}
		dbByteBuffer.stop();

		runParallel(threadsNumber, () -> {
			Database<String, String> threadDB = openDB(ENGINE, file1, new StringConverter());
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				threadDB.get(sb.substring(0, x), (String v) -> {
					assertEquals(v, sb.substring(0, x + 1));
				});
			}
			threadDB.stop();
		}, () -> {
			Database<ByteBuffer, ByteBuffer> threadDB = openDB(ENGINE, file2, new ByteBufferConverter());
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				threadDB.get(stringToByteBuffer(sb.substring(0, x)), (ByteBuffer v) -> {
					assertEquals(byteBufferToString(v), sb.substring(0, x + 1));
				});
			}
			threadDB.stop();
		});
	}
}
