/*
 * Copyright 2017-2018, Intel Corporation
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *
 *     * Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.pmem.pmemkv;

import java.nio.ByteBuffer;
import io.pmem.pmemkv.internal.*;

public class KVEngine {

    public KVEngine(String engine, String path) {
        pointer = kvengine_open(engine, path, 8388608);
    }

    public KVEngine(String engine, String path, long size) {
        pointer = kvengine_open(engine, path, size);
    }

    public void close() {
        if (!closed) {
            closed = true;
            kvengine_close(pointer);
        }
    }

    public boolean closed() {
        return closed;
    }

    public void all(AllBuffersCallback callback) {
        kvengine_all_buffers(pointer, (int keybytes, ByteBuffer key) -> {
            key.rewind();
            key.limit(keybytes);
            callback.process(key);
        });
    }

    public void all(AllByteArraysCallback callback) {
        kvengine_all_bytearrays(pointer, callback);
    }

    public void all(AllStringsCallback callback) {
        kvengine_all_strings(pointer, callback);
    }

    public long count() {
        return kvengine_count(pointer);
    }

    public void each(EachBufferCallback callback) {
        kvengine_each_buffer(pointer, (int keybytes, ByteBuffer key, int valuebytes, ByteBuffer value) -> {
            key.rewind();
            key.limit(keybytes);
            value.rewind();
            value.limit(valuebytes);
            callback.process(key, value);
        });
    }

    public void each(EachByteArrayCallback callback) {
        kvengine_each_bytearray(pointer, callback);
    }

    public void each(EachStringCallback callback) {
        kvengine_each_string(pointer, callback);
    }

    public boolean exists(ByteBuffer key) {
        return kvengine_exists_buffer(pointer, key.position(), key);
    }

    public boolean exists(byte[] key) {
        return kvengine_exists_bytes(pointer, key);
    }

    public boolean exists(String key) {
        return kvengine_exists_bytes(pointer, key.getBytes());
    }

    public void get(ByteBuffer key, ByteBuffer value) {
        int valuebytes = kvengine_get_buffer(pointer, key.position(), key, value.capacity(), value);
        value.rewind();
        value.limit(valuebytes);
    }

    public byte[] get(byte[] key) {
        return kvengine_get_bytes(pointer, key);
    }

    public String get(String key) {
        byte[] result = kvengine_get_bytes(pointer, key.getBytes());
        return result == null ? null : new String(result);
    }

    public void put(ByteBuffer key, ByteBuffer value) {
        try {
            kvengine_put_buffer(pointer, key.position(), key, value.position(), value);
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public void put(byte[] key, byte[] value) {
        try {
            kvengine_put_bytes(pointer, key, value);
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public void put(String key, String value) {
        try {
            kvengine_put_bytes(pointer, key.getBytes(), value.getBytes());
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public boolean remove(ByteBuffer key) {
        try {
            return kvengine_remove_buffer(pointer, key.position(), key);
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public boolean remove(byte[] key) {
        try {
            return kvengine_remove_bytes(pointer, key);
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    public boolean remove(String key) {
        try {
            return kvengine_remove_bytes(pointer, key.getBytes());
        } catch (KVEngineException kve) {
            kve.setKey(key);
            throw kve;
        }
    }

    private boolean closed;
    private final long pointer;

    // JNI METHODS --------------------------------------------------------------------------------

    private native long kvengine_open(String engine, String path, long size);

    private native void kvengine_close(long pointer);

    private native void kvengine_all_buffers(long pointer, AllBuffersJNICallback callback);

    private native void kvengine_all_bytearrays(long pointer, AllByteArraysCallback callback);

    private native void kvengine_all_strings(long pointer, AllStringsCallback callback);

    private native long kvengine_count(long pointer);

    private native void kvengine_each_buffer(long pointer, EachBufferJNICallback callback);

    private native void kvengine_each_bytearray(long pointer, EachByteArrayCallback callback);

    private native void kvengine_each_string(long pointer, EachStringCallback callback);

    private native boolean kvengine_exists_buffer(long pointer, int keybytes, ByteBuffer key);

    private native boolean kvengine_exists_bytes(long pointer, byte[] key);

    private native int kvengine_get_buffer(long pointer, int keybytes, ByteBuffer key, int valbytes, ByteBuffer val);

    private native byte[] kvengine_get_bytes(long pointer, byte[] key);

    private native void kvengine_put_buffer(long pointer, int keybytes, ByteBuffer key, int valbytes, ByteBuffer val);

    private native void kvengine_put_bytes(long pointer, byte[] key, byte[] value);

    private native boolean kvengine_remove_buffer(long pointer, int keybytes, ByteBuffer key);

    private native boolean kvengine_remove_bytes(long pointer, byte[] key);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
