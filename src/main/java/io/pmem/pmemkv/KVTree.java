/*
 * Copyright 2017, Intel Corporation
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

public class KVTree {

    public KVTree(String path, long size) {
        pointer = kvtree_open(path, size);
    }

    public void close() {
        if (!closed) {
            closed = true;
            kvtree_close(pointer);
        }
    }

    public boolean closed() {
        return closed;
    }

    public String get(String key) {
        return kvtree_get(pointer, key);
    }

    public void put(String key, String value) {
        kvtree_put(pointer, key, value);
    }

    public void remove(String key) {
        kvtree_remove(pointer, key);
    }

    public long size() {
        return kvtree_size(pointer);
    }

    private boolean closed;
    private final long pointer;

    // JNI METHODS --------------------------------------------------------------------------------

    private native long kvtree_open(String path, long size);

    private native void kvtree_close(long pointer);

    private native String kvtree_get(long pointer, String key);

    private native void kvtree_put(long pointer, String key, String value);

    private native void kvtree_remove(long pointer, String key);

    private native long kvtree_size(long pointer);

    static {
        System.loadLibrary("pmemkv-jni");
    }

}
