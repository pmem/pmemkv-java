// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.ByteBuffer;

import static io.pmem.pmemkv.TestUtils.*;

public class CmapTest {

	private final String ENGINE = "cmap";
	private String DB_PATH = "";

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(new File(TEST_DB_DIR));

	@Before
	public void init() {
		DB_PATH = testDir.getRoot() + File.separator + "testfile";
		assertTrue(DB_PATH != null && !DB_PATH.isEmpty());
	}

	@Test
	public void testCreateAndOpen() {
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, DB_PATH, new ByteBufferConverter());

		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");

		db.stop();

		db = openDB(ENGINE, DB_PATH, new ByteBufferConverter());
		assertTrue(db.exists(stringToByteBuffer("key1")));
		resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");
	}

	@Test
	public void throwsExceptionOnStartWhenOpeningNonExistentFile() {
		Database<ByteBuffer, ByteBuffer> db = null;

		try {
			db = openDB(ENGINE, DB_PATH, new ByteBufferConverter());
			Assert.fail();
		} catch (DatabaseException e) {
			/* file doesn't exist, open should throw */
		}

		assertNull(db);
	}

	@Test
	public void throwsExceptionOnSortedCountFuncs() {
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, DB_PATH, new ByteBufferConverter());
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

	/* Test the DB on multiple threads */
	@Test
	public void multipleThreadsDBTest() {
		final int threadsNumber = 8;
		final int numberOfElements = 100;

		final Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, DB_PATH, new ByteBufferConverter());

		runParallel(threadsNumber, () -> {
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				db.put(stringToByteBuffer(Integer.toString(x)),
						stringToByteBuffer(Integer.toString(x + 1)));
			}
		});

		runParallel(threadsNumber, () -> {
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				assertTrue(db.exists(stringToByteBuffer(Integer.toString(x))));
			}
		});

		runParallel(threadsNumber, () -> {
			for (int j = numberOfElements - 1; j >= 0; --j) {
				final int x = j;
				db.get(stringToByteBuffer(Integer.toString(x)), (ByteBuffer v) -> {
					assertEquals(byteBufferToString(v), Integer.toString(x + 1));
				});
			}
		});

		db.stop();
	}

	/* Use 2 different DBs simultaneously. */
	@Test
	public void multipleDBTypesTest() {
		final int threadsNumber = 8;
		final int numberOfElements = 200;

		String file1 = DB_PATH + "1";
		final Database<String, String> dbString = createDB(ENGINE, file1, new StringConverter(), 190, 190);
		final StringBuilder sb = new StringBuilder(numberOfElements + 1);
		sb.append('x');
		for (int i = 0; i < numberOfElements; ++i) {
			sb.append('x');
			dbString.put(sb.substring(0, i), sb.substring(0, i + 1));
		}

		String file2 = DB_PATH + "2";
		final Database<ByteBuffer, ByteBuffer> dbByteBuffer = createDB(ENGINE, file2, new ByteBufferConverter(), 180,
				180);
		for (int i = 0; i < numberOfElements; ++i) {
			dbByteBuffer.put(stringToByteBuffer(sb.substring(0, i)), stringToByteBuffer(sb.substring(0, i + 1)));
		}

		runParallel(threadsNumber, () -> {
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				dbString.get(sb.substring(0, x), (String v) -> {
					assertEquals(v, sb.substring(0, x + 1));
				});
			}
		}, () -> {
			for (int j = 0; j < numberOfElements; ++j) {
				final int x = j;
				dbByteBuffer.get(stringToByteBuffer(sb.substring(0, x)), (ByteBuffer v) -> {
					assertEquals(byteBufferToString(v), sb.substring(0, x + 1));
				});
			}
		});
		dbByteBuffer.stop();
		dbString.stop();
	}
}
