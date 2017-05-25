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

package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.KVTree;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.mscharhag.oleaster.matcher.Matchers.expect;

public class KVTreeTest {

    private final String PATH = "/dev/shm/pmemkv-java";
    private final long SIZE = 1024 * 1024 * 8;

    private void clean() {
        try {
            Files.deleteIfExists(Paths.get(PATH));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-42);
        }
    }

    @Before
    public void setUp() {
        clean();
    }

    @After
    public void tearDown() {
        clean();
    }

    @Test
    public void createsInstanceTest() {
        long size = 1024 * 1024 * 11;
        KVTree kv = new KVTree(PATH, size);
        expect(kv).toBeNotNull();
        expect(kv.closed()).toBeFalse();
        expect(kv.size()).toEqual(size);
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void createsInstanceFromExistingPoolTest() {
        long size = 1024 * 1024 * 13;
        KVTree kv = new KVTree(PATH, size);
        kv.close();
        kv = new KVTree(PATH, 0);
        expect(kv.closed()).toBeFalse();
        expect(kv.size()).toEqual(size);
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void closesInstanceMultipleTimesTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        expect(kv.closed()).toBeFalse();
        expect(kv.size()).toEqual(SIZE);
        kv.close();
        expect(kv.closed()).toBeTrue();
        kv.close();
        expect(kv.closed()).toBeTrue();
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void getsMissingKeyTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        expect(kv.get("key1")).toBeNull();
        kv.close();
    }

    @Test
    public void putsBasicValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "value1");
        expect(kv.get("key1")).toEqual("value1");
        kv.close();
    }

    @Test
    public void putsBinaryKeyTest() {
        // todo finish
    }

    @Test
    public void putsBinaryValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "A\0B\0\0C");
        expect(kv.get("key1")).toEqual("A\0B\0\0C");
        kv.close();
    }

    @Test
    public void putsComplexValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        String val = "one\ttwo or <p>three</p>\n {four}   and ^five";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsEmptyKeyTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("", "value1");
        expect(kv.get("")).toEqual("value1");
        kv.close();
    }

    @Test
    public void putsEmptyValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "");
        expect(kv.get("key1")).toEqual("");
        kv.close();
    }

    @Test
    public void putsMultipleValuesTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "value1");
        kv.put("key2", "value2");
        kv.put("key3", "value3");
        expect(kv.get("key1")).toEqual("value1");
        expect(kv.get("key2")).toEqual("value2");
        expect(kv.get("key3")).toEqual("value3");
        kv.close();
    }

    @Test
    public void putsOverwritingExistingValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "value1");
        expect(kv.get("key1")).toEqual("value1");
        kv.put("key1", "value123");
        expect(kv.get("key1")).toEqual("value123");
        kv.put("key1", "asdf");
        expect(kv.get("key1")).toEqual("asdf");
        kv.close();
    }

    @Test
    public void putsUtf8KeyTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        String val = "to remember, note, record";
        kv.put("记", val);
        expect(kv.get("记")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsUtf8ValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        String val = "记 means to remember, note, record";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsVeryLargeValueTest() {
        // todo finish
    }

    @Test
    public void removesKeyandValueTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        kv.put("key1", "value1");
        expect(kv.get("key1")).toEqual("value1");
        kv.remove("key1");
        expect(kv.get("key1")).toBeNull();
        kv.close();
    }

    @Test
    public void throwsExceptionOnCreateWhenPathIsInvalidTest() {
        KVTree kv = null;
        try {
            kv = new KVTree("/tmp/123/234/345/456/567/678/nope.nope", SIZE);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("unable to open persistent pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnCreateWithHugeSizeTest() {
        KVTree kv = null;
        try {
            kv = new KVTree(PATH, 9223372036854775807L); // 9.22 exabytes
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("unable to open persistent pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnCreateWithTinySizeTest() {
        KVTree kv = null;
        try {
            kv = new KVTree(PATH, SIZE - 1); // too small
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("unable to open persistent pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnPutWhenOutOfSpaceTest() {
        KVTree kv = new KVTree(PATH, SIZE);
        try {
            for (int i = 0; i < 100000; i++) {
                String istr = String.valueOf(i);
                kv.put(istr, istr);
            }
            Assert.fail();
        } catch (RuntimeException re) {
            expect(re.getMessage()).toEqual("unable to put value");
        } catch (Exception e) {
            Assert.fail();
        }
        kv.close();
    }

}