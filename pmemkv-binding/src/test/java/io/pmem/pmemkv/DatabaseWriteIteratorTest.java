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

	/* Helper method, used in most of the tests in this file */
	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		Database<ByteBuffer, ByteBuffer> db = openDB(engine, DB_DIR, new ByteBufferConverter());
		assertNotNull(db);
		assertFalse(db.stopped());
		return db;
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
		Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator();
		assertNotNull(it);
		assertTrue(it.seekToFirst());;
		assertTrue(byteBufferToString(it.key()).equals("key1"));
		db.stop();
		assertTrue(db.stopped());
	}

	@Test
	public void readFewEntryTest() {
		TreeMap<String, String> hs = new TreeMap<String, String>();
		hs.put("key1", "value1");
		hs.put("key2", "value2");
		hs.put("key3", "value3");

		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.exists(stringToByteBuffer("key1")));

		for (Map.Entry<String, String> entry : hs.entrySet()) {
			db.put(stringToByteBuffer(entry.getKey()), stringToByteBuffer(entry.getValue()));
		}

		Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator();
		assertNotNull(it);
		assertTrue(it.seekToFirst());
		assertTrue(it.isNext());
		int counter = 1;
		while (it.isNext()) {
			counter++;
			assertTrue(it.next());;
		}
		assertTrue(counter == hs.size());
		db.stop();
		assertTrue(db.stopped());
	}
}
