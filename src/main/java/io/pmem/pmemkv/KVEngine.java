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

    public void all(KVAllCallback callback) {
        kvengine_all(pointer, callback);
    }

    public void allStrings(KVAllStringsCallback callback) {
        kvengine_all_strings(pointer, callback);
    }

    public long count() {
        return kvengine_count(pointer);
    }

    public long countLike(String pattern) {
        return kvengine_count_like(pointer, pattern.getBytes());
    }

    public void each(KVEachCallback callback) {
        kvengine_each(pointer, callback);
    }

    public void eachLike(String pattern, KVEachCallback callback) {
        kvengine_each_like(pointer, pattern.getBytes(), callback);
    }

    public void eachString(KVEachStringCallback callback) {
        kvengine_each_string(pointer, callback);
    }

    public void eachStringLike(String pattern, KVEachStringCallback callback) {
        kvengine_each_string_like(pointer, pattern.getBytes(), callback);
    }

    public boolean exists(byte[] key) {
        return kvengine_exists(pointer, key);
    }

    public boolean exists(String key) {
        return kvengine_exists(pointer, key.getBytes());
    }

    public byte[] get(byte[] key) {
        return kvengine_get(pointer, key);
    }

    public String get(String key) {
        byte[] result = kvengine_get(pointer, key.getBytes());
        return result == null ? null : new String(result);
    }

    public void put(byte[] key, byte[] value) {
        kvengine_put(pointer, key, value);
    }

    public void put(String key, String value) {
        kvengine_put(pointer, key.getBytes(), value.getBytes());
    }

    public void remove(byte[] key) {
        kvengine_remove(pointer, key);
    }

    public void remove(String key) {
        kvengine_remove(pointer, key.getBytes());
    }

    private boolean closed;
    private final long pointer;

    // JNI METHODS --------------------------------------------------------------------------------

    private native long kvengine_open(String engine, String path, long size);

    private native void kvengine_close(long pointer);

    private native void kvengine_all(long pointer, KVAllCallback callback);

    private native void kvengine_all_strings(long pointer, KVAllStringsCallback callback);

    private native long kvengine_count(long pointer);

    private native long kvengine_count_like(long pointer, byte[] pattern);

    private native void kvengine_each(long pointer, KVEachCallback callback);

    private native void kvengine_each_like(long pointer, byte[] pattern, KVEachCallback callback);

    private native void kvengine_each_string(long pointer, KVEachStringCallback callback);

    private native void kvengine_each_string_like(long pointer, byte[] pattern,
                                                  KVEachStringCallback callback);

    private native boolean kvengine_exists(long pointer, byte[] key);

    private native byte[] kvengine_get(long pointer, byte[] key);

    private native void kvengine_put(long pointer, byte[] key, byte[] value);

    private native void kvengine_remove(long pointer, byte[] key);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
