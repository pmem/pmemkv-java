// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class DatabaseIteratorTest {

	private final String ENGINE = "vsmap";
	private String DB_DIR = "";

	/* Helper methods, used in most of the tests in this file */
	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		Database<ByteBuffer, ByteBuffer> db = TestUtils.openDB(engine, DB_DIR, new ByteBufferConverter());
		assertNotNull(db);
		assertFalse(db.stopped());
		return db;
	}

	private TreeMap<String, String> buildHashMapWithGaps(int size) {
		TreeMap<String, String> hs = new TreeMap<String, String>();
		int seed = 4;
		for (int i = 0; i < size; i++) {
			if (i % seed == 0)
				continue;
			hs.put("key" + i, "value" + i);
		}
		return hs;
	}

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(TestUtils.DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_DIR = testDir.getRoot().toString();
		assertTrue(DB_DIR != null && !DB_DIR.isEmpty());
	}

	/*
	 * XXX: change ByteBufferConverter to StringConverter to avoid conversion
	 * outside converter
	 */
	@Test
	public void readFirstEntryTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		assertFalse(db.exists(TestUtils.stringToByteBuffer("key1")));

		db.put(TestUtils.stringToByteBuffer("key1"), TestUtils.stringToByteBuffer("value1"));
		assertTrue(db.exists(TestUtils.stringToByteBuffer("key1")));
		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertNotNull(it);
			assertTrue(it.seekToFirst());
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key1"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value1"));
			assertFalse(it.isNext());
		}
		db.stop();
		assertTrue(db.stopped());
	}
	@Test
	public void readAllEntriesTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(1024);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.exists(TestUtils.stringToByteBuffer("key1")));

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertNotNull(it);
			assertTrue(it.seekToFirst());
			assertTrue(it.isNext());
			int counter = 1;
			while (it.isNext()) {
				counter++;
				String key = TestUtils.byteBufferToString(it.key());
				String value = TestUtils.byteBufferToString(it.value());
				/* key and value should have the same last character */
				assertTrue(key.substring(key.length() - 1).equals(value.substring(value.length() - 1)));
				assertTrue(it.next());
			}
			assertTrue(counter == hs.size());
		}
		db.stop();
	}

	@Test
	public void seekAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seek(TestUtils.stringToByteBuffer("key3")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key3"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekFailTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertFalse(it.seek(TestUtils.stringToByteBuffer("nope")));
		}
		db.stop();
	}

	@Test
	public void seekLowerAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekLower(TestUtils.stringToByteBuffer("key3")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key2"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value2"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekLowerFailTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertFalse(it.seekLower(TestUtils.stringToByteBuffer("key0")));
		}
		db.stop();
	}

	@Test
	public void seekLowerEqAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekLowerEq(TestUtils.stringToByteBuffer("key4")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key3"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekLowerEqWhenExistsAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekLowerEq(TestUtils.stringToByteBuffer("key3")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key3"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekHigher(TestUtils.stringToByteBuffer("key3")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key5"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value5"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherFailTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertFalse(it.seekHigher(TestUtils.stringToByteBuffer("key9")));
		}
		db.stop();
	}

	@Test
	public void seekHigherEqAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekHigherEq(TestUtils.stringToByteBuffer("key4")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key5"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value5"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherEqWhenExistsAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(TestUtils.stringToByteBuffer(entry.getKey()), TestUtils.stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.ReadIterator it = db.iterator()) {
			assertTrue(it.seekHigherEq(TestUtils.stringToByteBuffer("key3")));
			assertTrue(TestUtils.byteBufferToString(it.key()).equals("key3"));
			assertTrue(TestUtils.byteBufferToString(it.value()).equals("value3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(TestUtils.byteBufferToString(it.key())));
			}
		}
		db.stop();
	}
}
