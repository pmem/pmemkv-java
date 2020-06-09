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
        Database db = new Database("blackhole", CONFIG);
        expect(db.countAll()).toEqual(0);
        expect(db.exists("key1")).toBeFalse();
        expect(db.get("key1")).toBeNull();
        db.put("key1", "value1");
        expect(db.countAll()).toEqual(0);
        expect(db.exists("key1")).toBeFalse();
        expect(db.get("key1")).toBeNull();
        expect(db.remove("key1")).toBeTrue();
        expect(db.exists("key1")).toBeFalse();
        expect(db.get("key1")).toBeNull();
        db.stop();
    }

    @Test
    public void startEngineTest() {
        Database db = new Database(ENGINE, CONFIG);
        expect(db).toBeNotNull();
        expect(db.stopped()).toBeFalse();
        db.stop();
        expect(db.stopped()).toBeTrue();
    }

    @Test
    public void stopsEngineMultipleTimesTest() {
        Database db = new Database(ENGINE, CONFIG);
        expect(db.stopped()).toBeFalse();
        db.stop();
        expect(db.stopped()).toBeTrue();
        db.stop();
        expect(db.stopped()).toBeTrue();
        db.stop();
        expect(db.stopped()).toBeTrue();
    }

    @Test
    public void getsMissingKeyTest() {
        Database db = new Database(ENGINE, CONFIG);
        expect(db.exists("key1")).toBeFalse();
        expect(db.get("key1")).toBeNull();
        db.stop();
    }

    @Test
    public void putsBasicValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        expect(db.exists("key1")).toBeFalse();
        db.put("key1", "value1");
        expect(db.exists("key1")).toBeTrue();
        expect(db.get("key1")).toEqual("value1");
        db.stop();
    }

    @Test
    public void putsBinaryKeyTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A\0B\0\0C", "value1");
        expect(db.exists("A\0B\0\0C")).toBeTrue();
        expect(db.get("A\0B\0\0C")).toEqual("value1");
        db.put("1\02\0\03".getBytes(), "value123!".getBytes());
        expect(db.exists("1\02\0\03")).toBeTrue();
        expect(db.get("1\02\0\03")).toEqual("value123!");
        db.stop();
    }

    @Test
    public void putsBinaryValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("key1", "A\0B\0\0C");
        expect(db.get("key1")).toEqual("A\0B\0\0C");
        db.put("key2".getBytes(), "1\02\0\03!".getBytes());
        expect(db.get("key2")).toEqual("1\02\0\03!");
        db.stop();
    }

    @Test
    public void putsComplexValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        String val = "one\ttwo or <p>three</p>\n {four}   and ^five";
        db.put("key1", val);
        expect(db.get("key1")).toEqual(val);
        db.stop();
    }

    @Test
    public void putsEmptyKeyTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("", "empty");
        db.put(" ", "single-space");
        db.put("\t\t", "two-tab");
        expect(db.exists("")).toBeTrue();
        expect(db.get("")).toEqual("empty");
        expect(db.exists(" ")).toBeTrue();
        expect(db.get(" ")).toEqual("single-space");
        expect(db.exists("\t\t")).toBeTrue();
        expect(db.get("\t\t")).toEqual("two-tab");
        db.stop();
    }

    @Test
    public void putsEmptyValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("empty", "");
        db.put("single-space", " ");
        db.put("two-tab", "\t\t");
        expect(db.get("empty")).toEqual("");
        expect(db.get("single-space")).toEqual(" ");
        expect(db.get("two-tab")).toEqual("\t\t");
        db.stop();
    }

    @Test
    public void putsMultipleValuesTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("key1", "value1");
        db.put("key2", "value2");
        db.put("key3", "value3");
        expect(db.exists("key1")).toBeTrue();
        expect(db.get("key1")).toEqual("value1");
        expect(db.exists("key2")).toBeTrue();
        expect(db.get("key2")).toEqual("value2");
        expect(db.exists("key3")).toBeTrue();
        expect(db.get("key3")).toEqual("value3");
        db.stop();
    }

    @Test
    public void putsOverwritingExistingValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("key1", "value1");
        expect(db.get("key1")).toEqual("value1");
        db.put("key1", "value123");
        expect(db.get("key1")).toEqual("value123");
        db.put("key1", "asdf");
        expect(db.get("key1")).toEqual("asdf");
        db.stop();
    }

    @Test
    public void putsUtf8KeyTest() {
        Database db = new Database(ENGINE, CONFIG);
        String val = "to remember, note, record";
        db.put("记", val);
        expect(db.exists("记")).toBeTrue();
        expect(db.get("记")).toEqual(val);
        db.stop();
    }

    @Test
    public void putsUtf8ValueTest() {
        Database db = new Database(ENGINE, CONFIG);
        String val = "记 means to remember, note, record";
        db.put("key1", val);
        expect(db.get("key1")).toEqual(val);
        db.stop();
    }

    public void removesKeyandValueTest() {
        Database db = new Database(ENGINE, CONFIG);

        db.put("key1", "value1");
        expect(db.exists("key1")).toBeTrue();
        expect(db.get("key1")).toEqual("value1");
        expect(db.remove("key1")).toBeTrue();
        expect(db.remove("key1")).toBeFalse();
        expect(db.exists("key1")).toBeFalse();
        expect(db.get("key1")).toBeNull();

        db.put("key1", "value1");
        expect(db.exists("key1".getBytes())).toBeTrue();
        expect(db.get("key1".getBytes())).toEqual("value1");
        expect(db.remove("key1".getBytes())).toBeTrue();
        expect(db.remove("key1".getBytes())).toBeFalse();
        expect(db.exists("key1".getBytes())).toBeFalse();
        expect(db.get("key1".getBytes())).toBeNull();
        db.stop();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsEmptyTest() {
        Database db = null;
        try {
            db = new Database(ENGINE, "{}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenConfigIsMalformedTest() {
        Database db = null;
        try {
            db = new Database(ENGINE, "{");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenEngineIsInvalidTest() {
        Database db = null;
        try {
            db = new Database("nope.nope", CONFIG);
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsInvalidTest() {
        Database db = null;
        try {
            db = new Database(ENGINE, "{\"path\":\"/tmp/123/234/345/456/567/678/nope.nope\"}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsWrongTypeTest() {
        Database db = null;
        try {
            db = new Database(ENGINE, "{\"path\":1234}");
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void usesGetKeysTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("1", "one");
        db.put("2", "two");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.getKeys((String k) -> x.append("<").append(k).append(">,"));
        expect(x.toString()).toEqual("<1>,<2>,<记!>,");

        StringBuilder x2 = new StringBuilder();
        db.getKeys((byte[] k) -> x2.append("<").append(new String(k)).append(">,"));
        expect(x2.toString()).toEqual("<1>,<2>,<记!>,");

        StringBuilder x3 = new StringBuilder();
        db.getKeys((ByteBuffer k) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,"));
        expect(x3.toString()).toEqual("<1>,<2>,<记!>,");

        db.stop();
    }

    @Test
    public void usesGetKeysAboveTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.getKeysAbove("B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("BB,BC,记!,");

        StringBuilder x2 = new StringBuilder();
        db.getKeysAbove("".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        db.getKeysAbove(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        db.stop();
    }

    @Test
    public void usesGetKeysBelowTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.getKeysBelow("B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("A,AB,AC,");

        StringBuilder x2 = new StringBuilder();
        db.getKeysBelow("\uFFFF".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        db.getKeysBelow(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        db.stop();
    }

    @Test
    public void usesGetKeysBetweenTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.getKeysBetween("A", "B", (String k) -> x.append(k).append(","));
        expect(x.toString()).toEqual("AB,AC,");

        StringBuilder x2 = new StringBuilder();
        db.getKeysBetween("B".getBytes(), "\uFFFF".getBytes(), (byte[] k) -> x2.append(new String(k)).append(","));
        expect(x2.toString()).toEqual("BB,BC,记!,");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        db.getKeysBetween(key1b, key2b, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        StringBuilder x4 = new StringBuilder();
        db.getKeysBetween("", "", (String k) -> x4.append(k).append(","));
        db.getKeysBetween("A", "A", (String k) -> x4.append(k).append(","));
        db.getKeysBetween("B", "A", (String k) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        db.stop();
    }

    @Test
    public void usesCountTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("BD", "7");
        expect(db.countAll()).toEqual(7);

        expect(db.countAbove("")).toEqual(7);
        expect(db.countAbove("A")).toEqual(6);
        expect(db.countAbove("B")).toEqual(3);
        expect(db.countAbove("BC")).toEqual(1);
        expect(db.countAbove("BD")).toEqual(0);
        expect(db.countAbove("Z")).toEqual(0);

        expect(db.countBelow("")).toEqual(0);
        expect(db.countBelow("A")).toEqual(0);
        expect(db.countBelow("B")).toEqual(3);
        expect(db.countBelow("BD")).toEqual(6);
        expect(db.countBelow("ZZZZZ")).toEqual(7);

        expect(db.countBetween("", "ZZZZ")).toEqual(7);
        expect(db.countBetween("", "A")).toEqual(0);
        expect(db.countBetween("", "B")).toEqual(3);
        expect(db.countBetween("A", "B")).toEqual(2);
        expect(db.countBetween("B", "ZZZZ")).toEqual(3);

        expect(db.countBetween("", "")).toEqual(0);
        expect(db.countBetween("A", "A")).toEqual(0);
        expect(db.countBetween("AC", "A")).toEqual(0);
        expect(db.countBetween("B", "A")).toEqual(0);
        expect(db.countBetween("BD", "A")).toEqual(0);
        expect(db.countBetween("ZZZ", "B")).toEqual(0);

        expect(db.countAbove("A".getBytes())).toEqual(6);
        expect(db.countBelow("B".getBytes())).toEqual(3);
        expect(db.countBetween("".getBytes(), "B".getBytes())).toEqual(3);

        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("BD".getBytes());
        expect(db.countAbove(key1b)).toEqual(3);
        expect(db.countBelow(key2b)).toEqual(6);
        expect(db.countBetween(key1b, key2b)).toEqual(2);

        db.stop();
    }

    @Test
    public void usesGetAllTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("1", "one");
        db.put("2", "two");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.get_all((String k, String v) -> x.append("<").append(k).append(">,<").append(v).append(">|"));
        expect(x.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        StringBuilder x2 = new StringBuilder();
        db.get_all((byte[] k, byte[] v) -> x2.append("<").append(new String(k)).append(">,<")
                .append(new String(v)).append(">|"));
        expect(x2.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        StringBuilder x3 = new StringBuilder();
        db.get_all((ByteBuffer k, ByteBuffer v) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,<")
                .append(UTF_8.decode(v).toString()).append(">|"));
        expect(x3.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        db.stop();
    }

    @Test
    public void usesGetAllAboveTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.get_above("B", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x2 = new StringBuilder();
        db.get_above("".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        db.get_above(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        db.stop();
    }

    @Test
    public void usesGetAllBelowTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.get_below("AC", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("A,1|AB,2|");

        StringBuilder x2 = new StringBuilder();
        db.get_below("\uFFFF".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        db.get_below(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        db.stop();
    }

    @Test
    public void usesGetAllBetweenTest() {
        Database db = new Database(ENGINE, CONFIG);
        db.put("A", "1");
        db.put("AB", "2");
        db.put("AC", "3");
        db.put("B", "4");
        db.put("BB", "5");
        db.put("BC", "6");
        db.put("记!", "RR");

        StringBuilder x = new StringBuilder();
        db.get_between("A", "B", (String k, String v) -> x.append(k).append(",").append(v).append("|"));
        expect(x.toString()).toEqual("AB,2|AC,3|");

        StringBuilder x2 = new StringBuilder();
        db.get_between("B".getBytes(), "\uFFFF".getBytes(), (byte[] k, byte[] v) -> x2.append(new String(k)).append(",")
                .append(new String(v)).append("|"));
        expect(x2.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        db.get_between(key1b, key2b, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x4 = new StringBuilder();
        db.get_between("", "", (String k, String v) -> x4.append(k).append(","));
        db.get_between("A", "A", (String k, String v) -> x4.append(k).append(","));
        db.get_between("B", "A", (String k, String v) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        db.stop();
    }

    @Test
    public void usesBuffersTest() {
        Database db = new Database(ENGINE, CONFIG);

        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        ByteBuffer valb = ByteBuffer.allocateDirect(1000);
        keyb.putInt(123);
        valb.putInt(234);
        expect(db.exists(keyb)).toBeFalse();
        db.put(keyb, valb);
        expect(db.exists(keyb)).toBeTrue();
        expect(db.countAll()).toEqual(1);

        keyb.clear();
        keyb.putInt(5678);
        valb.clear();
        valb.putInt(6789);
        expect(db.exists(keyb)).toBeFalse();
        db.put(keyb, valb);
        expect(db.exists(keyb)).toBeTrue();
        expect(db.countAll()).toEqual(2);

        AtomicInteger count = new AtomicInteger(0);
        db.get_all((ByteBuffer kb, ByteBuffer vb) -> {
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
        db.get(keyb, valb);
        expect(valb.limit()).toEqual(4);
        expect(valb.position()).toEqual(0);
        expect(valb.getInt()).toEqual(6789);

        expect(db.exists(keyb)).toBeTrue();
        expect(db.remove(keyb)).toBeTrue();
        expect(db.exists(keyb)).toBeFalse();
        expect(db.remove(keyb)).toBeFalse();

        db.stop();
    }

    @Test
    public void usesGetBufferIsDirectBufferTest() {
        Database db = new Database(ENGINE, CONFIG);

        ByteBuffer keyb = ByteBuffer.allocateDirect(3);
        ByteBuffer valb = ByteBuffer.allocateDirect(3);
        keyb.put("k".getBytes());
        keyb.put("v".getBytes());

        db.put(keyb, valb);
        db.get(keyb, (ByteBuffer v) -> expect(v.isDirect()).toBeTrue());
    }
}
