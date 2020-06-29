// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.Converter;

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

public class StringExample{

    public static void main(String[] args) {
        String ENGINE = "vsmap";

        System.out.println("Starting engine");

        Database<String, String> db = new Database.Builder<String, String>(ENGINE).
                setSize(1073741824).
                setPath("/dev/shm").
                setKeyConverter(new StringConverter()).
                setValueConverter(new StringConverter()).
                build();

        System.out.println("Putting new key");
        db.put("key1", "value1");
        assert db.countAll() == 1;

        System.out.println("Reading key back");
        assert db.getCopy("key1").equals("value1");

        System.out.println("Iterating existing keys");
        db.put("key2", "value2");
        db.put("key3", "value3");
        db.getKeys((k) -> System.out.println("  visited: " + k));

        System.out.println("Removing existing key");
        db.remove("key1");
        assert !db.exists("key1");

        System.out.println("Stopping engine");
        db.stop();
    }
}
