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
import io.pmem.pmemkv.KVEngineException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static junit.framework.TestCase.fail;

public class KVEngineTest {

    private final String ENGINE = "kvtree3";
    private final String PATH = "/dev/shm/pmemkv-java";
    private final long SIZE = 1024 * 1024 * 8;
    private final String CONFIG = "{\"path\":\"" + PATH + "\",\"size\":" + SIZE + "}";

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
    public void blackholeTest() {
        KVEngine kv = new KVEngine("blackhole", CONFIG);
        expect(kv.count()).toEqual(0);
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        kv.put("key1", "value1");
        expect(kv.count()).toEqual(0);
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        expect(kv.remove("key1")).toBeTrue();
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        kv.stop();
    }

    @Test
    public void startEngineTest() {
        long size = 1024 * 1024 * 11;
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv).toBeNotNull();
        expect(kv.stopped()).toBeFalse();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
    }

    @Test
    public void startEngineWithExistingPoolTest() {
        long size = 1024 * 1024 * 13;
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.stop();
        kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.stopped()).toBeFalse();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
    }

    @Test
    public void stopsEngineMultipleTimesTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.stopped()).toBeFalse();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
    }

    @Test
    public void getsMissingKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        kv.stop();
    }

    @Test
    public void putsBasicValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.exists("key1")).toBeFalse();
        kv.put("key1", "value1");
        expect(kv.exists("key1")).toBeTrue();
        expect(kv.get("key1")).toEqual("value1");
        kv.stop();
    }

    @Test
    public void putsBinaryKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("A\0B\0\0C", "value1");
        expect(kv.exists("A\0B\0\0C")).toBeTrue();
        expect(kv.get("A\0B\0\0C")).toEqual("value1");
        kv.put("1\02\0\03".getBytes(), "value123!".getBytes());
        expect(kv.exists("1\02\0\03")).toBeTrue();
        expect(kv.get("1\02\0\03")).toEqual("value123!");
        kv.stop();
    }

    @Test
    public void putsBinaryValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("key1", "A\0B\0\0C");
        expect(kv.get("key1")).toEqual("A\0B\0\0C");
        kv.put("key2".getBytes(), "1\02\0\03!".getBytes());
        expect(kv.get("key2")).toEqual("1\02\0\03!");
        kv.stop();
    }

    @Test
    public void putsComplexValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        String val = "one\ttwo or <p>three</p>\n {four}   and ^five";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.stop();
    }

    @Test
    public void putsEmptyKeyTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("", "empty");
        kv.put(" ", "single-space");
        kv.put("\t\t", "two-tab");
        expect(kv.exists("")).toBeTrue();
        expect(kv.get("")).toEqual("empty");
        expect(kv.exists(" ")).toBeTrue();
        expect(kv.get(" ")).toEqual("single-space");
        expect(kv.exists("\t\t")).toBeTrue();
        expect(kv.get("\t\t")).toEqual("two-tab");
        kv.stop();
    }

    @Test
    public void putsEmptyValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("empty", "");
        kv.put("single-space", " ");
        kv.put("two-tab", "\t\t");
        expect(kv.get("empty")).toEqual("");
        expect(kv.get("single-space")).toEqual(" ");
        expect(kv.get("two-tab")).toEqual("\t\t");
        kv.stop();
    }

    @Test
    public void putsMultipleValuesTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("key1", "value1");
        kv.put("key2", "value2");
        kv.put("key3", "value3");
        expect(kv.exists("key1")).toBeTrue();
        expect(kv.get("key1")).toEqual("value1");
        expect(kv.exists("key2")).toBeTrue();
        expect(kv.get("key2")).toEqual("value2");
        expect(kv.exists("key3")).toBeTrue();
        expect(kv.get("key3")).toEqual("value3");
        kv.stop();
    }

    @Test
    public void putsOverwritingExistingValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("key1", "value1");
        expect(kv.get("key1")).toEqual("value1");
        kv.put("key1", "value123");
        expect(kv.get("key1")).toEqual("value123");
        kv.put("key1", "asdf");
        expect(kv.get("key1")).toEqual("asdf");
        kv.stop();
    }

    @Test
    public void putsUtf8KeyTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        String val = "to remember, note, record";
        kv.put("记", val);
        expect(kv.exists("记")).toBeTrue();
        expect(kv.get("记")).toEqual(val);
        kv.stop();
    }

    @Test
    public void putsUtf8ValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        String val = "记 means to remember, note, record";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.stop();
    }

    @Test
    public void putsVeryLargeValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        String val = "ABCDEFGHIJLMNOPQRSTUZWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.stop();
    }

    @Test
    public void recoversManyValuesTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            kv.put(s, s);
        }
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            expect(kv.get(s)).toEqual(s);
        }
        kv.put("test123", "123");
        kv.stop();
        kv = new KVEngine(ENGINE, CONFIG);
        for (int i = 0; i < 6000; i++) {
            String s = String.valueOf(i);
            expect(kv.get(s)).toEqual(s);
        }
        expect(kv.get("test123")).toEqual("123");
        kv.stop();
    }

    public void removesKeyandValueTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        kv.put("key1", "value1");
        expect(kv.exists("key1")).toBeTrue();
        expect(kv.get("key1")).toEqual("value1");
        expect(kv.remove("key1")).toBeTrue();
        expect(kv.remove("key1")).toBeFalse();
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        kv.stop();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsEmptyTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{}");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Config does not include valid path string");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsMalformedTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Config could not be parsed as JSON");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenEngineIsInvalidTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine("nope.nope", CONFIG);
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Unknown engine name");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsInvalidTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"path\":\"/tmp/123/234/345/456/567/678/nope.nope\",\"size\":" + SIZE + "}");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed creating pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsMissingTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"size\":" + SIZE + "}");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Config does not include valid path string");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsWrongTypeTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"path\":1234}");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Config does not include valid path string");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenSizeIsWrongTypeTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"path\":\"" + PATH + "\",\"size\":\"" + SIZE + "\"}");
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Config does not include valid size integer");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWithHugeSizeTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"path\":\"" + PATH + "\",\"size\":9223372036854775807}"); // 9.22 exabytes
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed creating pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWithTinySizeTest() {
        KVEngine kv = null;
        try {
            kv = new KVEngine(ENGINE, "{\"path\":\"" + PATH + "\",\"size\":" + (SIZE - 1) + "}"); // too small
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed creating pool");
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnPutWhenOutOfSpaceTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        try {
            for (int i = 0; i < 100000; i++) {
                String istr = String.valueOf(i);
                kv.put(istr, istr);
            }
            Assert.fail();
        } catch (KVEngineException kve) {
            expect(kve.getKey()).toBeNotNull();
            expect(kve.getKey()).toBeInstanceOf(String.class);
            expect(kve.getMessage()).toEqual("Unable to put key");
        } catch (Exception e) {
            Assert.fail();
        }
        kv.stop();
    }

    @Test
    public void usesAllByteArraysTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.count()).toEqual(0);
        kv.put("RR".getBytes(), "BBB".getBytes());
        expect(kv.count()).toEqual(1);
        kv.put("1".getBytes(), "2".getBytes());
        expect(kv.count()).toEqual(2);
        StringBuilder s = new StringBuilder();
        kv.all((byte[] k) -> s.append("<").append(new String(k)).append(">,"));
        expect(s.toString()).toEqual("<1>,<RR>,");
        kv.stop();
    }

    @Test
    public void usesAllStringsTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.count()).toEqual(0);
        kv.put("记!", "RR");
        expect(kv.count()).toEqual(1);
        kv.put("2", "one");
        expect(kv.count()).toEqual(2);
        StringBuilder s = new StringBuilder();
        kv.all((String k) -> s.append("<").append(k).append(">,"));
        expect(s.toString()).toEqual("<2>,<记!>,");
        kv.stop();
    }

    @Test
    public void usesEachByteArrayTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.count()).toEqual(0);
        kv.put("RR".getBytes(), "BBB".getBytes());
        expect(kv.count()).toEqual(1);
        kv.put("1".getBytes(), "2".getBytes());
        expect(kv.count()).toEqual(2);
        StringBuilder s = new StringBuilder();
        kv.each((byte[] k, byte[] v) -> s.append("<").append(new String(k)).append(">,<")
                .append(new String(v)).append(">|"));
        expect(s.toString()).toEqual("<1>,<2>|<RR>,<BBB>|");
        kv.stop();
    }

    @Test
    public void usesEachStringTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);
        expect(kv.count()).toEqual(0);
        kv.put("red", "记!");
        expect(kv.count()).toEqual(1);
        kv.put("one", "2");
        expect(kv.count()).toEqual(2);
        StringBuilder s = new StringBuilder();
        kv.each((String k, String v) -> s.append("<").append(k).append(">,<").append(v).append(">|"));
        expect(s.toString()).toEqual("<one>,<2>|<red>,<记!>|");
        kv.stop();
    }

    @Test
    public void usesBuffersTest() {
        KVEngine kv = new KVEngine(ENGINE, CONFIG);

        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        ByteBuffer valb = ByteBuffer.allocateDirect(1000);
        keyb.putInt(123);
        valb.putInt(234);
        expect(kv.exists(keyb)).toBeFalse();
        kv.put(keyb, valb);
        expect(kv.exists(keyb)).toBeTrue();
        expect(kv.count()).toEqual(1);

        keyb.clear();
        keyb.putInt(5678);
        valb.clear();
        valb.putInt(6789);
        expect(kv.exists(keyb)).toBeFalse();
        kv.put(keyb, valb);
        expect(kv.exists(keyb)).toBeTrue();
        expect(kv.count()).toEqual(2);

        try {
            kv.all((ByteBuffer bb) -> {
                throw new RuntimeException("Blech");
            });
            fail();
        } catch (RuntimeException re) {
            expect(re.getMessage()).toEqual("Blech");
        }

        AtomicInteger count = new AtomicInteger(0);
        kv.all((ByteBuffer kb) -> count.addAndGet(kb.getInt()));
        expect(count.intValue()).toEqual(5801);

        count.set(0);
        kv.each((ByteBuffer kb, ByteBuffer vb) -> {
            count.addAndGet(kb.getInt());
            count.addAndGet(vb.getInt());
        });
        expect(count.intValue()).toEqual(12824);

        valb.clear();
        valb.putInt(42);
        valb.putInt(42);
        valb.putInt(42);
        valb.putInt(42);
        expect(valb.position()).toEqual(16);
        kv.get(keyb, valb);
        expect(valb.limit()).toEqual(4);
        expect(valb.position()).toEqual(0);
        expect(valb.getInt()).toEqual(6789);

        expect(kv.exists(keyb)).toBeTrue();
        expect(kv.remove(keyb)).toBeTrue();
        expect(kv.exists(keyb)).toBeFalse();
        expect(kv.remove(keyb)).toBeFalse();

        kv.stop();
    }

}