// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CmapTest {

	private final String ENGINE = "cmap";

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

		assertThrows(DatabaseException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = openDB(ENGINE, file);
		});
	}

	@Test
	public void testConfigRelease() {
		Database<ByteBuffer, ByteBuffer> db = null;
		String file = folder.getRoot() + File.pathSeparator + "testfile";
		long size = 8388608;
		boolean startError = false;
		try {
			db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(size)
					.setPath(file)
					.setKeyConverter(new ByteBufferConverter())
					.setValueConverter(new ByteBufferConverter())
					.build();
		} catch (DatabaseException e) {
			startError = true;
		}
		if (startError) {
			startError = false;
			try {
				db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
						.setSize(size)
						.setPath(file)
						.setKeyConverter(new ByteBufferConverter())
						.setValueConverter(new ByteBufferConverter())
						.setForceCreate(true)
						.build();
			} catch (DatabaseException e) {
				startError = true;
			}
		}
		assertFalse(startError);
		assertFalse(db == null);
		db.stop();
		assertTrue(db.stopped());
	}
}
