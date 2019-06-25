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

package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.DatabaseException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.fail;

public class DatabaseTest {

    private final String ENGINE = "vsmap";
    private final String CONFIG = "{\"path\":\"/dev/shm\", \"size\":1073741824}";

    @Test
    public void blackholeTest() {
        Database kv = new Database("blackhole", CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
        expect(kv).toBeNotNull();
        expect(kv.stopped()).toBeFalse();
        kv.stop();
        expect(kv.stopped()).toBeTrue();
    }

    @Test
    public void stopsEngineMultipleTimesTest() {
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();
        kv.stop();
    }

    @Test
    public void putsBasicValueTest() {
        Database kv = new Database(ENGINE, CONFIG);
        expect(kv.exists("key1")).toBeFalse();
        kv.put("key1", "value1");
        expect(kv.exists("key1")).toBeTrue();
        expect(kv.get("key1")).toEqual("value1");
        kv.stop();
    }

    @Test
    public void putsBinaryKeyTest() {
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("key1", "A\0B\0\0C");
        expect(kv.get("key1")).toEqual("A\0B\0\0C");
        kv.put("key2".getBytes(), "1\02\0\03!".getBytes());
        expect(kv.get("key2")).toEqual("1\02\0\03!");
        kv.stop();
    }

    @Test
    public void putsComplexValueTest() {
        Database kv = new Database(ENGINE, CONFIG);
        String val = "one\ttwo or <p>three</p>\n {four}   and ^five";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.stop();
    }

    @Test
    public void putsEmptyKeyTest() {
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
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
        Database kv = new Database(ENGINE, CONFIG);
        String val = "to remember, note, record";
        kv.put("记", val);
        expect(kv.exists("记")).toBeTrue();
        expect(kv.get("记")).toEqual(val);
        kv.stop();
    }

    @Test
    public void putsUtf8ValueTest() {
        Database kv = new Database(ENGINE, CONFIG);
        String val = "记 means to remember, note, record";
        kv.put("key1", val);
        expect(kv.get("key1")).toEqual(val);
        kv.stop();
    }

    public void removesKeyandValueTest() {
        Database kv = new Database(ENGINE, CONFIG);

        kv.put("key1", "value1");
        expect(kv.exists("key1")).toBeTrue();
        expect(kv.get("key1")).toEqual("value1");
        expect(kv.remove("key1")).toBeTrue();
        expect(kv.remove("key1")).toBeFalse();
        expect(kv.exists("key1")).toBeFalse();
        expect(kv.get("key1")).toBeNull();

        kv.put("key1", "value1");
        expect(kv.exists("key1".getBytes())).toBeTrue();
        expect(kv.get("key1".getBytes())).toEqual("value1");
        expect(kv.remove("key1".getBytes())).toBeTrue();
        expect(kv.remove("key1".getBytes())).toBeFalse();
        expect(kv.exists("key1".getBytes())).toBeFalse();
        expect(kv.get("key1".getBytes())).toBeNull();
        kv.stop();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsEmptyTest() {
        Database kv = null;
        try {
            kv = new Database(ENGINE, "{}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed to open pmemkv"); // XXX
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsMalformedTest() {
        Database kv = null;
        try {
            kv = new Database(ENGINE, "{");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("JSON parsing error"); // XXX
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenEngineIsInvalidTest() {
        Database kv = null;
        try {
            kv = new Database("nope.nope", CONFIG);
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed to open pmemkv"); // XXX
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsInvalidTest() {
        Database kv = null;
        try {
            kv = new Database(ENGINE, "{\"path\":\"/tmp/123/234/345/456/567/678/nope.nope\"}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("Failed to open pmemkv"); // XXX
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsWrongTypeTest() {
        Database kv = null;
        try {
            kv = new Database(ENGINE, "{\"path\":1234}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
            expect(kve.getMessage()).toEqual("JSON parsing error"); // XXX
        } catch (Exception e) {
            Assert.fail();
        }
        expect(kv).toBeNull();
    }

    @Test
    public void usesAllTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("1", "one");
        kv.put("2", "two");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.all((String k) -> x.append("<").append(k).append(">,"));
        expect(x.toString()).toEqual("<1>,<2>,<记!>,");

        StringBuilder x2 = new StringBuilder();
        kv.all((byte[] k) -> x2.append("<").append(new String(k)).append(">,"));
        expect(x2.toString()).toEqual("<1>,<2>,<记!>,");

        StringBuilder x3 = new StringBuilder();
        kv.all((ByteBuffer k) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,"));
        expect(x3.toString()).toEqual("<1>,<2>,<记!>,");

        kv.stop();
    }

    @Test
    public void usesAllAboveTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.allAbove("B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("BB,BC,记!,");

        StringBuilder x2 = new StringBuilder();
        kv.allAbove("".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        kv.allAbove(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        kv.stop();
    }

    @Test
    public void usesAllBelowTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.allBelow("B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("A,AB,AC,");

        StringBuilder x2 = new StringBuilder();
        kv.allBelow("\uFFFF".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        kv.allBelow(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        kv.stop();
    }

    @Test
    public void usesAllBetweenTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.allBetween("A", "B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("AB,AC,");

        StringBuilder x2 = new StringBuilder();
        kv.allBetween("B".getBytes(), "\uFFFF".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        kv.allBetween(key1b, key2b, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        StringBuilder x4 = new StringBuilder();
        kv.allBetween("", "", (String k) -> x4.append(k).append(","));
        kv.allBetween("A", "A", (String k) -> x4.append(k).append(","));
        kv.allBetween("B", "A", (String k) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        kv.stop();
    }

    @Test
    public void usesCountTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("BD", "7");
        expect(kv.count()).toEqual(7);

        expect(kv.countAbove("")).toEqual(7);
        expect(kv.countAbove("A")).toEqual(6);
        expect(kv.countAbove("B")).toEqual(3);
        expect(kv.countAbove("BC")).toEqual(1);
        expect(kv.countAbove("BD")).toEqual(0);
        expect(kv.countAbove("Z")).toEqual(0);

        expect(kv.countBelow("")).toEqual(0);
        expect(kv.countBelow("A")).toEqual(0);
        expect(kv.countBelow("B")).toEqual(3);
        expect(kv.countBelow("BD")).toEqual(6);
        expect(kv.countBelow("ZZZZZ")).toEqual(7);

        expect(kv.countBetween("", "ZZZZ")).toEqual(7);
        expect(kv.countBetween("", "A")).toEqual(0);
        expect(kv.countBetween("", "B")).toEqual(3);
        expect(kv.countBetween("A", "B")).toEqual(2);
        expect(kv.countBetween("B", "ZZZZ")).toEqual(3);

        expect(kv.countBetween("", "")).toEqual(0);
        expect(kv.countBetween("A", "A")).toEqual(0);
        expect(kv.countBetween("AC", "A")).toEqual(0);
        expect(kv.countBetween("B", "A")).toEqual(0);
        expect(kv.countBetween("BD", "A")).toEqual(0);
        expect(kv.countBetween("ZZZ", "B")).toEqual(0);

        expect(kv.countAbove("A".getBytes())).toEqual(6);
        expect(kv.countBelow("B".getBytes())).toEqual(3);
        expect(kv.countBetween("".getBytes(), "B".getBytes())).toEqual(3);

        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("BD".getBytes());
        expect(kv.countAbove(key1b)).toEqual(3);
        expect(kv.countBelow(key2b)).toEqual(6);
        expect(kv.countBetween(key1b, key2b)).toEqual(2);

        kv.stop();
    }

    @Test
    public void usesEachTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("1", "one");
        kv.put("2", "two");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.each((String k, String v) -> x.append("<").append(k).append(">,<").append(v).append(">|"));
        expect(x.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        StringBuilder x2 = new StringBuilder();
        kv.each((byte[] k, byte[] v) -> x2.append("<").append(new String(k)).append(">,<")
                .append(new String(v)).append(">|"));
        expect(x2.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        StringBuilder x3 = new StringBuilder();
        kv.each((ByteBuffer k, ByteBuffer v) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,<")
                .append(UTF_8.decode(v).toString()).append(">|"));
        expect(x3.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        kv.stop();
    }

    @Test
    public void usesEachAboveTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.eachAbove("B", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x2 = new StringBuilder();
        kv.eachAbove("".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        kv.eachAbove(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        kv.stop();
    }

    @Test
    public void usesEachBelowTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.eachBelow("AC", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("A,1|AB,2|");

        StringBuilder x2 = new StringBuilder();
        kv.eachBelow("\uFFFF".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        kv.eachBelow(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        kv.stop();
    }

    @Test
    public void usesEachBetweenTest() {
        Database kv = new Database(ENGINE, CONFIG);
        kv.put("A", "1");
        kv.put("AB", "2");
        kv.put("AC", "3");
        kv.put("B", "4");
        kv.put("BB", "5");
        kv.put("BC", "6");
        kv.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        kv.eachBetween("A", "B", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("AB,2|AC,3|");

        StringBuilder x2 = new StringBuilder();
        kv.eachBetween("B".getBytes(), "\uFFFF".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        kv.eachBetween(key1b, key2b, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x4 = new StringBuilder();
        kv.eachBetween("", "", (String k, String v) -> x4.append(k).append(","));
        kv.eachBetween("A", "A", (String k, String v) -> x4.append(k).append(","));
        kv.eachBetween("B", "A", (String k, String v) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        kv.stop();
    }

    @Test
    public void usesBuffersTest() {
        Database kv = new Database(ENGINE, CONFIG);

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
