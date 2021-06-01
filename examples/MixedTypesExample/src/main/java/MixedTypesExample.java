// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.Converter;
import io.pmem.pmemkv.ByteBufferConverter;

import java.nio.ByteBuffer;

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

public class MixedTypesExample {
	public static void main(String[] args) {
		String ENGINE = "vsmap";

		/*
		 * keys are String - Converter is implemented above; values are ByteBuffer -
		 * ByteBufferConverter is already delivered in io.pmem.pmemkv package
		 */
		Database<String, ByteBuffer> db = new Database.Builder<String, ByteBuffer>(ENGINE)
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new StringConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();

		for (int i = 0; i < 0xFF; i++) {
			ByteBuffer value = ByteBuffer.allocateDirect(4);
			value.putInt(i);
			String key = "str" + i;
			db.put(key, value);
		}
		/* Simply read all data using getAll() with a lambda callback */
		db.getAll((k, v) -> {
			System.out.println("Key: " + k +
					" Value: " + String.format("0x%02X", v.getInt()));
		});

		db.stop();
	}
}
