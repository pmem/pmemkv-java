// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

interface Callback {
	void call();
}

class TestUtils {
	public static class StringConverter implements Converter<String> {
		public ByteBuffer toByteBuffer(String entry) {
			return ByteBuffer.wrap(entry.getBytes());
		}

		public String fromByteBuffer(ByteBuffer entry) {
			byte[] bytes;
			bytes = new byte[entry.capacity()];
			entry.get(bytes);
			return new String(bytes);
		}
	}

	public static ByteBuffer stringToByteBuffer(String msg) {
		return ByteBuffer.wrap(msg.getBytes());
	}

	public static String byteBufferToString(ByteBuffer buffer) {
		byte[] bytes;
		bytes = new byte[buffer.capacity()];
		buffer.get(bytes);
		return new String(bytes);
	}

	public static void runParallel(int numberOfThreads, Callback... functions) {
		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < functions.length; i++) {
			for (int j = 0; j < numberOfThreads / functions.length; j++) {
				final Callback function = functions[i];
				threads.add(new Thread() {
					public void run() {
						function.call();
					}
				});
			}
		}
		for (Thread t : threads) {
			t.run();
		}
		for (Thread t : threads) {
			try {
				t.join();
			} catch (Exception e) {
				assertTrue(false);
			}
		}
	}

	public static <KV> Database<KV, KV> createDB(String engine, String path, Converter<KV> kvConverter) {
		return createDB(engine, path, kvConverter, 10000000, 10000000);
	}

	public static <KV> Database<KV, KV> createDB(String engine, String path, Converter<KV> kvConverter,
			int keyBufferSize, int valBufferSize) {
		return new Database.Builder<KV, KV>(engine)
				.setSize(1073741824)
				.setForceCreate(true)
				.setPath(path)
				.setKeyConverter(kvConverter)
				.setValueConverter(kvConverter)
				.setKeyBufferSize(keyBufferSize)
				.setValueBufferSize(valBufferSize)
				.build();
	}

	public static <KV> Database<KV, KV> openDB(String engine, String path, Converter<KV> kvConverter) {
		return openDB(engine, path, kvConverter, 10000000, 10000000);
	}

	public static <KV> Database<KV, KV> openDB(String engine, String path, Converter<KV> kvConverter, int keyBufferSize,
			int valBufferSize) {
		return new Database.Builder<KV, KV>(engine)
				.setForceCreate(false)
				.setPath(path)
				.setKeyConverter(kvConverter)
				.setValueConverter(kvConverter)
				.setKeyBufferSize(keyBufferSize)
				.setValueBufferSize(valBufferSize)
				.build();
	}
}
