// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

import io.pmem.pmemkv.ByteBufferConverter;
import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.DatabaseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CmapTest {

	private final String ENGINE = "cmap";
	private Database<ByteBuffer, ByteBuffer> db;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Database<ByteBuffer, ByteBuffer> createDB(String engine, String path) {
		return new Database.Builder<ByteBuffer, ByteBuffer>(engine)
				.setSize(100000000).setForceCreate(true)
				.setPath(path)
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();
	}

	private Database<ByteBuffer, ByteBuffer> openDB(String engine, String path) {
		return new Database.Builder<ByteBuffer, ByteBuffer>(engine)
				.setForceCreate(false)
				.setPath(path)
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();
	}

	private static ByteBuffer stringToByteBuffer(String msg) {
		return ByteBuffer.wrap(msg.getBytes());
	}

	private static String byteBufferToString(ByteBuffer buffer) {
		byte[] bytes;
		bytes = new byte[buffer.capacity()];
		buffer.get(bytes);
		return new String(bytes);
	}

	@Test
	public void testCreateAndOpen() {
		String file = folder.getRoot() + File.pathSeparator + "testfile";
		Database<ByteBuffer, ByteBuffer> db = createDB(ENGINE, file);

		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");

		db.stop();

		db = openDB(ENGINE, file);
		assertTrue(db.exists(stringToByteBuffer("key1")));
		resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");
	}

	@Test
	public void throwsExceptionOnStartWhenOpeningNonExistentFile() {
		String file = folder.getRoot() + File.pathSeparator + "testfile";

		Database<ByteBuffer, ByteBuffer> db = null;

		try {
			db = openDB(ENGINE, file);
			Assert.fail();
		} catch (DatabaseException e) {

		}

		assertNull(db);
	}
}
