// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2021, Intel Corporation */

import io.pmem.pmemkv.Converter;
import io.pmem.pmemkv.Database;

import java.nio.ByteBuffer;

/* Implementation of Converter interface to allow
 * storing (in Database) keys and values as Strings.
 */
class StringConverter implements Converter<String> {
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

public class IteratorExample {
	public static void main(String[] args) {
		/*
		 * We pick sorted engine in this example to show the usage of seekHigherEq
		 * method. Unsorted engines do not support seekLower[Eq]/Higher[Eq] methods.
		 */
		String ENGINE = "vsmap";

		System.out.println("Starting engine");
		Database<String, String> db = new Database.Builder<String, String>(ENGINE)
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new StringConverter())
				.setValueConverter(new StringConverter())
				.build();

		System.out.println("Putting only odd keys");
		for (int i = 0; i <= 10; i++) {
			if (i % 2 == 0)
				continue;
			db.put("key" + i, "value" + i);
		}

		/* Create iterator using try-with-resources statement */
		try (Database<String, String>.ReadIterator it = db.readIterator()) {
			assert it != null;

			System.out.println("Seek first record and read it");
			boolean succeeded = it.seekToFirst();
			assert succeeded;

			/* Can't manipulate data in DB using key() or value() ! */
			String key = it.key();
			String value = it.value();
			System.out.println("Key: " + key + " and its value: " + value);
			assert key.equals("key1");
			assert value.equals("value1");

			System.out.println("Skip to record higher or equal to 'key2'");
			it.seekHigherEq("key2");
			key = it.key();
			value = it.value();
			System.out.println("Key: " + key + " and its value: " + value);

			System.out.println("Iterate until the end");
			while (it.isNext()) {
				it.next();
				key = it.key();
				value = it.value();
				System.out.println("Key: " + key + " and its value: " + value);
			}

			/* No need to close the iterator manually, it implements AutoCloseable */
		}

		System.out.println("Stopping engine");
		db.stop();
	}
}
