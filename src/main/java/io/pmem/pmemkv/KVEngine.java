/*
 * Copyright 2017-2019, Intel Corporation
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

import io.pmem.pmemkv.internal.AllBuffersJNICallback;
import io.pmem.pmemkv.internal.EachBufferJNICallback;

import java.nio.ByteBuffer;

public class KVEngine {

    public KVEngine(String engine, String config) {
        pointer = kvengine_start(engine, config);
    }

    public void stop() {
        if (!stopped) {
            stopped = true;
            kvengine_stop(pointer);
        }
    }

    public boolean stopped() {
        return stopped;
    }

    public void all(AllBuffersCallback callback) {
        kvengine_all_buffer(pointer, (ByteBuffer k, int kb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void all(AllByteArraysCallback callback) {
        kvengine_all_bytes(pointer, callback);
    }

    public void all(AllStringsCallback callback) {
        kvengine_all_string(pointer, callback);
    }

    public void allAbove(ByteBuffer key, AllBuffersCallback callback) {
        kvengine_all_above_buffer(pointer, key, key.position(), (ByteBuffer k, int kb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void allAbove(byte[] key, AllByteArraysCallback callback) {
        kvengine_all_above_bytes(pointer, key, callback);
    }

    public void allAbove(String key, AllStringsCallback callback) {
        kvengine_all_above_string(pointer, key.getBytes(), callback);
    }

    public void allBelow(ByteBuffer key, AllBuffersCallback callback) {
        kvengine_all_below_buffer(pointer, key, key.position(), (ByteBuffer k, int kb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void allBelow(byte[] key, AllByteArraysCallback callback) {
        kvengine_all_below_bytes(pointer, key, callback);
    }

    public void allBelow(String key, AllStringsCallback callback) {
        kvengine_all_below_string(pointer, key.getBytes(), callback);
    }

    public void allBetween(ByteBuffer key1, ByteBuffer key2, AllBuffersCallback callback) {
        kvengine_all_between_buffer(pointer, key1, key1.position(), key2, key2.position(), (ByteBuffer k, int kb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb)));
    }

    public void allBetween(byte[] key1, byte[] key2, AllByteArraysCallback callback) {
        kvengine_all_between_bytes(pointer, key1, key2, callback);
    }

    public void allBetween(String key1, String key2, AllStringsCallback callback) {
        kvengine_all_between_string(pointer, key1.getBytes(), key2.getBytes(), callback);
    }

    public long count() {
        return kvengine_count(pointer);
    }

    public long countAbove(ByteBuffer key) {
        return kvengine_count_above_buffer(pointer, key, key.position());
    }

    public long countAbove(byte[] key) {
        return kvengine_count_above_bytes(pointer, key);
    }

    public long countAbove(String key) {
        return kvengine_count_above_bytes(pointer, key.getBytes());
    }

    public long countBelow(ByteBuffer key) {
        return kvengine_count_below_buffer(pointer, key, key.position());
    }

    public long countBelow(byte[] key) {
        return kvengine_count_below_bytes(pointer, key);
    }

    public long countBelow(String key) {
        return kvengine_count_below_bytes(pointer, key.getBytes());
    }

    public long countBetween(ByteBuffer key1, ByteBuffer key2) {
        return kvengine_count_between_buffer(pointer, key1, key1.position(), key2, key2.position());
    }

    public long countBetween(byte[] key1, byte[] key2) {
        return kvengine_count_between_bytes(pointer, key1, key2);
    }

    public long countBetween(String key1, String key2) {
        return kvengine_count_between_bytes(pointer, key1.getBytes(), key2.getBytes());
    }

    public void each(EachBufferCallback callback) {
        kvengine_each_buffer(pointer, (ByteBuffer k, int kb, ByteBuffer v, int vb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void each(EachByteArrayCallback callback) {
        kvengine_each_bytes(pointer, callback);
    }

    public void each(EachStringCallback callback) {
        kvengine_each_string(pointer, callback);
    }

    public void eachAbove(ByteBuffer key, EachBufferCallback callback) {
        kvengine_each_above_buffer(pointer, key, key.position(), (ByteBuffer k, int kb, ByteBuffer v, int vb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void eachAbove(byte[] key, EachByteArrayCallback callback) {
        kvengine_each_above_bytes(pointer, key, callback);
    }

    public void eachAbove(String key, EachStringCallback callback) {
        kvengine_each_above_string(pointer, key.getBytes(), callback);
    }

    public void eachBelow(ByteBuffer key, EachBufferCallback callback) {
        kvengine_each_below_buffer(pointer, key, key.position(), (ByteBuffer k, int kb, ByteBuffer v, int vb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void eachBelow(byte[] key, EachByteArrayCallback callback) {
        kvengine_each_below_bytes(pointer, key, callback);
    }

    public void eachBelow(String key, EachStringCallback callback) {
        kvengine_each_below_string(pointer, key.getBytes(), callback);
    }

    public void eachBetween(ByteBuffer key1, ByteBuffer key2, EachBufferCallback callback) {
        kvengine_each_between_buffer(pointer, key1, key1.position(), key2, key2.position(), (ByteBuffer k, int kb, ByteBuffer v, int vb)
                -> callback.process((ByteBuffer) k.rewind().limit(kb), (ByteBuffer) v.rewind().limit(vb)));
    }

    public void eachBetween(byte[] key1, byte[] key2, EachByteArrayCallback callback) {
        kvengine_each_between_bytes(pointer, key1, key2, callback);
    }

    public void eachBetween(String key1, String key2, EachStringCallback callback) {
        kvengine_each_between_string(pointer, key1.getBytes(), key2.getBytes(), callback);
    }

    public boolean exists(ByteBuffer key) {
        return kvengine_exists_buffer(pointer, key, key.position());
    }

    public boolean exists(byte[] key) {
        return kvengine_exists_bytes(pointer, key);
    }

    public boolean exists(String key) {
        return kvengine_exists_bytes(pointer, key.getBytes());
    }

    public void get(ByteBuffer key, ByteBuffer value) {
        int valuebytes = kvengine_get_buffer(pointer, key, key.position(), value, value.capacity());
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
            kvengine_put_buffer(pointer, key, key.position(), value, value.position());
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
            return kvengine_remove_buffer(pointer, key, key.position());
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

    private final long pointer;
    private boolean stopped;

    // JNI METHODS --------------------------------------------------------------------------------

    private native long kvengine_start(String engine, String config);
    private native void kvengine_stop(long ptr);
    private native void kvengine_all_buffer(long ptr, AllBuffersJNICallback cb);
    private native void kvengine_all_bytes(long ptr, AllByteArraysCallback cb);
    private native void kvengine_all_string(long ptr, AllStringsCallback cb);
    private native void kvengine_all_above_buffer(long ptr, ByteBuffer k, int kb, AllBuffersJNICallback cb);
    private native void kvengine_all_above_bytes(long ptr, byte[] k, AllByteArraysCallback cb);
    private native void kvengine_all_above_string(long ptr, byte[] k, AllStringsCallback cb);
    private native void kvengine_all_below_buffer(long ptr, ByteBuffer k, int kb, AllBuffersJNICallback cb);
    private native void kvengine_all_below_bytes(long ptr, byte[] k, AllByteArraysCallback cb);
    private native void kvengine_all_below_string(long ptr, byte[] k, AllStringsCallback cb);
    private native void kvengine_all_between_buffer(long ptr, ByteBuffer k1, int kb1, ByteBuffer k2, int kb2, AllBuffersJNICallback cb);
    private native void kvengine_all_between_bytes(long ptr, byte[] k1, byte[] k2, AllByteArraysCallback cb);
    private native void kvengine_all_between_string(long ptr, byte[] k1, byte[] k2, AllStringsCallback cb);
    private native long kvengine_count(long ptr);
    private native long kvengine_count_above_buffer(long ptr, ByteBuffer k, int kb);
    private native long kvengine_count_above_bytes(long ptr, byte[] k);
    private native long kvengine_count_below_buffer(long ptr, ByteBuffer k, int kb);
    private native long kvengine_count_below_bytes(long ptr, byte[] k);
    private native long kvengine_count_between_buffer(long ptr, ByteBuffer k1, int kb1, ByteBuffer k2, int kb2);
    private native long kvengine_count_between_bytes(long ptr, byte[] k1, byte[] k2);
    private native void kvengine_each_buffer(long ptr, EachBufferJNICallback cb);
    private native void kvengine_each_bytes(long ptr, EachByteArrayCallback cb);
    private native void kvengine_each_string(long ptr, EachStringCallback cb);
    private native void kvengine_each_above_buffer(long ptr, ByteBuffer k, int kb, EachBufferJNICallback cb);
    private native void kvengine_each_above_bytes(long ptr, byte[] k, EachByteArrayCallback cb);
    private native void kvengine_each_above_string(long ptr, byte[] k, EachStringCallback cb);
    private native void kvengine_each_below_buffer(long ptr, ByteBuffer k, int kb, EachBufferJNICallback cb);
    private native void kvengine_each_below_bytes(long ptr, byte[] k, EachByteArrayCallback cb);
    private native void kvengine_each_below_string(long ptr, byte[] k, EachStringCallback cb);
    private native void kvengine_each_between_buffer(long ptr, ByteBuffer k1, int kb1, ByteBuffer k2, int kb2, EachBufferJNICallback cb);
    private native void kvengine_each_between_bytes(long ptr, byte[] k1, byte[] k2, EachByteArrayCallback cb);
    private native void kvengine_each_between_string(long ptr, byte[] k1, byte[] k2, EachStringCallback cb);
    private native boolean kvengine_exists_buffer(long ptr, ByteBuffer k, int kb);
    private native boolean kvengine_exists_bytes(long ptr, byte[] k);
    private native int kvengine_get_buffer(long ptr, ByteBuffer k, int kb, ByteBuffer v, int vb);
    private native byte[] kvengine_get_bytes(long ptr, byte[] k);
    private native void kvengine_put_buffer(long ptr, ByteBuffer k, int kb, ByteBuffer v, int vb);
    private native void kvengine_put_bytes(long ptr, byte[] k, byte[] v);
    private native boolean kvengine_remove_buffer(long ptr, ByteBuffer k, int kb);
    private native boolean kvengine_remove_bytes(long ptr, byte[] k);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
