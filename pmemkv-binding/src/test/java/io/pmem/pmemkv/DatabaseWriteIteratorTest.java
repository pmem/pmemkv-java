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

import static io.pmem.pmemkv.TestUtils.*;

public class DatabaseWriteIteratorTest {

	private final String ENGINE = "vsmap";
	private String DB_DIR = "";

	/* Helper method, used in most of the tests in this file */
	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		return openDB(engine, DB_DIR, new ByteBufferConverter());
	}

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_DIR = testDir.getRoot().toString();
		assertTrue(DB_DIR != null && !DB_DIR.isEmpty());
	}

	@After
	public void finish() {

	}

	@Test
	public void startEngineTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertNotNull(db);
		assertFalse(db.stopped());

		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		Database<ByteBuffer, ByteBuffer>.WriteIterator it = db.iterator();
		it.seek_to_first();
		db.stop();
		assertTrue(db.stopped());
	}
}
