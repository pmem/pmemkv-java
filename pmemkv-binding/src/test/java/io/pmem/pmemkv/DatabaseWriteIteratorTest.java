// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import static io.pmem.pmemkv.TestUtils.*;

public class DatabaseWriteIteratorTest {

	private final String ENGINE = "vsmap";
	private String DB_DIR = "";

	/* Helper methods, used in most of the tests in this file */
	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		Database<ByteBuffer, ByteBuffer> db = openDB(engine, DB_DIR, new ByteBufferConverter());
		assertNotNull(db);
		assertFalse(db.stopped());
		return db;
	}

	private TreeMap<String, String> buildHashMapWithGaps(int size) {
		TreeMap<String, String> hs = new TreeMap<String, String>();
		int seed = 4;
		for (int i = 0; i < 10; i++) {
			if (i % seed == 0)
				continue;
			hs.put("key" + i, "value" + i);
		}
		return hs;
	}

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_DIR = testDir.getRoot().toString();
		assertTrue(DB_DIR != null && !DB_DIR.isEmpty());
	}

	@Test
	public void readFirstEntryTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertNotNull(it);
			assertTrue(it.seekToFirst());;
			assertTrue(byteBufferToString(it.key()).equals("key1"));
		}
		db.stop();
		assertTrue(db.stopped());
	}

	@Test
	public void readFewEntryTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.exists(stringToByteBuffer("key1")));

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertNotNull(it);
			assertTrue(it.seekToFirst());
			assertTrue(it.isNext());
			int counter = 1;
			while (it.isNext()) {
				counter++;
				assertTrue(it.next());;
			}
			assertTrue(counter == hs.size());
		}
		db.stop();
		assertTrue(db.stopped());
	}

	@Test
	public void seekAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seek(stringToByteBuffer("key3")));
			assertTrue(byteBufferToString(it.key()).equals("key3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekLowerAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekLower(stringToByteBuffer("key3")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key2"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekLowerEqAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekLowerEq(stringToByteBuffer("key4")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekLowerEqWhenExistsAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekLowerEq(stringToByteBuffer("key3")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekHigher(stringToByteBuffer("key3")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key5"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherEqAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekHigherEq(stringToByteBuffer("key4")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key5"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}

	@Test
	public void seekHigherEqWhenExistsAndReadKeyTest() {
		TreeMap<String, String> hs = buildHashMapWithGaps(10);
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		try (Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator()) {
			assertTrue(it.seekHigherEq(stringToByteBuffer("key3")));
			if (ENGINE.equals("vsmap"))
				assertTrue(byteBufferToString(it.key()).equals("key3"));
			while (it.isNext()) {
				assertTrue(it.next());
				assertTrue(hs.containsKey(byteBufferToString(it.key())));
			}
		}
		db.stop();
	}
}
