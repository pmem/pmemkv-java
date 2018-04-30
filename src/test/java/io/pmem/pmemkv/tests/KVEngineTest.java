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

package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.KVEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.mscharhag.oleaster.matcher.Matchers.expect;

public class KVEngineTest {

    private final String ENGINE = "kvtree2";
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
        KVEngine kv = new KVEngine(ENGINE, PATH, size);
        expect(kv).toBeNotNull();
        expect(kv.closed()).toBeFalse();
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void createsInstanceFromExistingPoolTest() {
        long size = 1024 * 1024 * 13;
        KVEngine kv = new KVEngine(ENGINE, PATH, size);
        kv.close();
        kv = new KVEngine(ENGINE, PATH, 0);
        expect(kv.closed()).toBeFalse();
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void closesInstanceMultipleTimesTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        expect(kv.closed()).toBeFalse();
        kv.close();
        expect(kv.closed()).toBeTrue();
        kv.close();
        expect(kv.closed()).toBeTrue();
        kv.close();
        expect(kv.closed()).toBeTrue();
    }

    @Test
    public void getsMissingKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        expect(kv.getString("key1")).toBeNull();
        kv.close();
    }

    @Test
    public void putsBasicValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("key1", "value1");
        expect(kv.getString("key1")).toEqual("value1");
        kv.close();
    }

    @Test
    public void putsBinaryKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("A\0B\0\0C", "value1");
        expect(kv.getString("A\0B\0\0C")).toEqual("value1");
        kv.put("1\02\0\03".getBytes(), "value123!".getBytes());
        expect(kv.getString("1\02\0\03")).toEqual("value123!");
        kv.close();
    }

    @Test
    public void putsBinaryValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("key1", "A\0B\0\0C");
        expect(kv.getString("key1")).toEqual("A\0B\0\0C");
        kv.put("key2".getBytes(), "1\02\0\03!".getBytes());
        expect(kv.getString("key2")).toEqual("1\02\0\03!");
        kv.close();
    }

    @Test
    public void putsComplexValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        String val = "one\ttwo or <p>three</p>\n {four}   and ^five";
        kv.putString("key1", val);
        expect(kv.getString("key1")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsEmptyKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("", "empty");
        kv.putString(" ", "single-space");
        kv.putString("\t\t", "two-tab");
        expect(kv.getString("")).toEqual("empty");
        expect(kv.getString(" ")).toEqual("single-space");
        expect(kv.getString("\t\t")).toEqual("two-tab");
        kv.close();
    }

    @Test
    public void putsEmptyValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("empty", "");
        kv.putString("single-space", " ");
        kv.putString("two-tab", "\t\t");
        expect(kv.getString("empty")).toEqual("");
        expect(kv.getString("single-space")).toEqual(" ");
        expect(kv.getString("two-tab")).toEqual("\t\t");
        kv.close();
    }

    @Test
    public void putsMultipleValuesTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("key1", "value1");
        kv.putString("key2", "value2");
        kv.putString("key3", "value3");
        expect(kv.getString("key1")).toEqual("value1");
        expect(kv.getString("key2")).toEqual("value2");
        expect(kv.getString("key3")).toEqual("value3");
        kv.close();
    }

    @Test
    public void putsOverwritingExistingValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("key1", "value1");
        expect(kv.getString("key1")).toEqual("value1");
        kv.putString("key1", "value123");
        expect(kv.getString("key1")).toEqual("value123");
        kv.putString("key1", "asdf");
        expect(kv.getString("key1")).toEqual("asdf");
        kv.close();
    }

    @Test
    public void putsUtf8KeyTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        String val = "to remember, note, record";
        kv.putString("记", val);
        expect(kv.getString("记")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsUtf8ValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        String val = "记 means to remember, note, record";
        kv.putString("key1", val);
        expect(kv.getString("key1")).toEqual(val);
        kv.close();
    }

    @Test
    public void putsVeryLargeValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        String val = "ABCDEFGHIJLMNOPQRSTUZWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        kv.putString("key1", val);
        expect(kv.getString("key1")).toEqual(val);
        kv.close();
    }

    @Test
    public void recoversManyValuesTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            kv.putString(s, s);
        }
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            expect(kv.getString(s)).toEqual(s);
        }
        kv.putString("test123", "123");
        kv.close();
        kv = new KVEngine(ENGINE, PATH, SIZE);
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            expect(kv.getString(s)).toEqual(s);
        }
        expect(kv.getString("test123")).toEqual("123");
        kv.close();
    }

    @Test
    public void removesKeyandValueTest() {
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        kv.putString("key1", "value1");
        expect(kv.getString("key1")).toEqual("value1");
        kv.remove("key1");
        expect(kv.getString("key1")).toBeNull();
        kv.close();
    }

    @Test
    public void throwsExceptionOnCreateWhenEngineIsInvalidTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine("nope.nope", PATH, SIZE);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("unable to open persistent pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnCreateWhenPathIsInvalidTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "/tmp/123/234/345/456/567/678/nope.nope", SIZE);
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
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, PATH, 9223372036854775807L); // 9.22 exabytes
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
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, PATH, SIZE - 1); // too small
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
        KVEngine kv = new KVEngine(ENGINE, PATH, SIZE);
        try {
            for (int i = 0; i < 100000; i++) {
                String istr = String.valueOf(i);
                kv.putString(istr, istr);
            }
            Assert.fail();
        } catch (RuntimeException re) {
            expect(re.getMessage()).toStartWith("unable to put key:");
        } catch (Exception e) {
            Assert.fail();
        }
        kv.close();
    }

    @Test
    public void usesBlackholeEngineTest() {
        KVEngine kv = new KVEngine("blackhole", PATH, SIZE);
        expect(kv.getString("key1")).toBeNull();
        kv.putString("key1", "value1");
        expect(kv.getString("key1")).toBeNull();
        kv.remove("key1");
        expect(kv.getString("key1")).toBeNull();
        kv.close();
    }

}