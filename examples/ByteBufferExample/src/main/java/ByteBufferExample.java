// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.ByteBufferConverter;

import java.nio.ByteBuffer;

public class ByteBufferExample {
	public static void main(String[] args) {
		String ENGINE = "vsmap";

		/*
		 * Keys and values are ByteBuffer; ByteBufferConverter is already delivered in
		 * io.pmem.pmemkv package
		 */
		Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();

		/* Direct ByteBuffer */
		for (int i = 0; i < 0xFF; i++) {
			ByteBuffer key = ByteBuffer.allocateDirect(4);
			key.putInt(i);
			db.put(key, key);
		}
		/* Simply read all data using getAll() with a lambda callback */
		db.getAll((ByteBuffer k, ByteBuffer v) -> {
			System.out.println("Key: " + String.format("0x%02X", k.getInt()) +
					" Value: " + String.format("0x%02X", v.getInt()));
		});

		db.stop();
	}
}
