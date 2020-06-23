// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2020, Intel Corporation */

package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.DatabaseException;
import io.pmem.pmemkv.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.fail;

class ByteBufferConverter implements Converter<ByteBuffer> {
    public ByteBuffer toByteBuffer(ByteBuffer entry) {
      return entry;
    }

    public ByteBuffer fromByteBuffer(ByteBuffer entry) {
      return entry;
    }
}

public class DatabaseTest {

    private final String ENGINE = "vsmap";

    private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
        return new Database.Builder(engine).
                setSize(1073741824).
                setPath("/dev/shm").
                setKeyConverter(new ByteBufferConverter()).
                setValueConverter(new ByteBufferConverter()).
                <ByteBuffer, ByteBuffer>build();
    }

    private static ByteBuffer stringToByteBuffer(String msg){
        return ByteBuffer.wrap(msg.getBytes());
    }

    private static String byteBufferToString(ByteBuffer buffer){
        byte[] bytes;
        bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return new String(bytes);
    }

    @Test
    public void blackholeTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB("blackhole");
        expect(db.countAll()).toEqual(0);
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        ByteBuffer ret =  db.getCopy(stringToByteBuffer("key1"));
        expect(db.getCopy(stringToByteBuffer("key1"))).toBeNull();
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
        expect(db.countAll()).toEqual(0);
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        expect(db.getCopy(stringToByteBuffer("key1"))).toBeNull();
        expect(db.remove(stringToByteBuffer("key1"))).toBeTrue();
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        expect(db.getCopy(stringToByteBuffer("key1"))).toBeNull();
        db.stop();
    }

    @Test
    public void startEngineTest() {
        Database db = buildDB(ENGINE);
        expect(db).toBeNotNull();
        expect(db.stopped()).toBeFalse();
        db.stop();
        expect(db.stopped()).toBeTrue();
    }

    @Test
    public void stopsEngineMultipleTimesTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
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
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        expect(db.getCopy(stringToByteBuffer("key1"))).toBeNull();
        db.stop();
    }

    @Test
    public void putsBasicValueTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
        expect(db.exists(stringToByteBuffer("key1"))).toBeTrue();
        ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
        expect(byteBufferToString(resBuff)).toEqual("value1");
        db.stop();
    }

    @Test
    public void putsEmptyKeyTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer(""), stringToByteBuffer("empty"));
        db.put(stringToByteBuffer(" "), stringToByteBuffer("single-space"));
        db.put(stringToByteBuffer("\t\t"), stringToByteBuffer("two-tab"));
        expect(db.exists(stringToByteBuffer(""))).toBeTrue();
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("")))).toEqual("empty");
        expect(db.exists(stringToByteBuffer(" "))).toBeTrue();
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer(" ")))).toEqual("single-space");
        expect(db.exists(stringToByteBuffer("\t\t"))).toBeTrue();
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("\t\t")))).toEqual("two-tab");
        db.stop();
    }

    @Test
    public void putsEmptyValueTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("empty"), stringToByteBuffer(""));
        db.put(stringToByteBuffer("single-space"), stringToByteBuffer(" "));
        db.put(stringToByteBuffer("two-tab"), stringToByteBuffer("\t\t"));
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("empty")))).toEqual("");
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("single-space")))).toEqual(" ");
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("two-tab")))).toEqual("\t\t");
        db.stop();
    }

    @Test
    public void putsMultipleValuesTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
        db.put(stringToByteBuffer("key2"), stringToByteBuffer("value2"));
        db.put(stringToByteBuffer("key3"), stringToByteBuffer("value3"));
        expect(db.exists(stringToByteBuffer("key1"))).toBeTrue();
        expect(byteBufferToString(
            db.getCopy(stringToByteBuffer("key1")))).toEqual("value1");
        expect(db.exists(stringToByteBuffer("key2"))).toBeTrue();
        expect(byteBufferToString(
            db.getCopy(stringToByteBuffer("key2")))).toEqual("value2");
        expect(db.exists(stringToByteBuffer("key3"))).toBeTrue();
        expect(byteBufferToString(
            db.getCopy(stringToByteBuffer("key3")))).toEqual("value3");
        expect(db.countAll()).toEqual(3);
        db.stop();
    }

    @Test
    public void putsOverwritingExistingValueTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("key1")))).toEqual("value1");
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value123"));
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("key1")))).toEqual("value123");
        db.put(stringToByteBuffer("key1"), stringToByteBuffer("asdf"));
        expect(byteBufferToString(
                db.getCopy(stringToByteBuffer("key1")))).toEqual("asdf");
        db.stop();
    }

    @Test
    public void removesKeyandValueTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

        db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
        expect(db.exists(stringToByteBuffer("key1"))).toBeTrue();
        db.get(stringToByteBuffer("key1"), (ByteBuffer v) -> {
            expect(byteBufferToString(v)).toEqual("value1");
        });
        expect(db.remove(stringToByteBuffer("key1"))).toBeTrue();
        expect(db.remove(stringToByteBuffer("key1"))).toBeFalse();
        expect(db.exists(stringToByteBuffer("key1"))).toBeFalse();
        expect(db.getCopy(stringToByteBuffer("key1"))).toBeNull();

        db.stop();
    }

    @Test
    public void throwsExceptionOnStartWhenPathIsMissing() {
        Database db = null;
        try {
            db = new Database.Builder(ENGINE).
                setSize(1073741824).
                <ByteBuffer, ByteBuffer>build();
            Assert.fail();
        } catch (DatabaseException kve) {
            expect(kve.getKey()).toBeNull();
        } catch (Exception e) {
            Assert.fail();
        }
        expect(db).toBeNull();
    }

    @Test
    public void throwsExceptionOnStartWhenSizeIsMissing() {
        Database db = null;
        try {
            db = new Database.Builder(ENGINE).
                setPath("/dev/shm").
                <ByteBuffer, ByteBuffer>build();
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
            db = buildDB("nope.nope");
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
            db = new Database.Builder(ENGINE).
                setSize(1073741824).
                setPath("/tmp/123/234/345/456/567/678/nope.nope").
                <ByteBuffer, ByteBuffer>build();
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
            db = new Database.Builder(ENGINE).
                setSize(1073741824).
                setPath("1234").
                <ByteBuffer, ByteBuffer>build();
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
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("1"), stringToByteBuffer("one"));
        db.put(stringToByteBuffer("2"), stringToByteBuffer("two"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        expect(db.countAll()).toEqual(3);
        StringBuilder x3 = new StringBuilder();
        db.getKeys((ByteBuffer k) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,"));
        expect(x3.toString()).toEqual("<1>,<2>,<记!>,");

        db.stop();
    }

    @Test
    public void usesGetKeysAboveTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        db.getKeysAbove(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        db.stop();
    }

    @Test
    public void usesGetKeysBelowTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        db.getKeysBelow(keyb, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("A,AB,AC,B,BB,BC,记!,");

        db.stop();
    }

    @Test
    public void usesGetKeysBetweenTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        db.getKeysBetween(key1b, key2b, (ByteBuffer k) -> x3.append(UTF_8.decode(k).toString()).append(","));
        expect(x3.toString()).toEqual("BB,BC,记!,");

        StringBuilder x4 = new StringBuilder();
        db.getKeysBetween(stringToByteBuffer(""), stringToByteBuffer(""), (ByteBuffer k) -> x4.append(k).append(","));
        db.getKeysBetween(stringToByteBuffer("A"), stringToByteBuffer("A"), (ByteBuffer k) -> x4.append(k).append(","));
        db.getKeysBetween(stringToByteBuffer("B"), stringToByteBuffer("A"), (ByteBuffer k) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        db.stop();
    }

    @Test
    public void usesCountTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("BD"), stringToByteBuffer("7"));
        expect(db.countAll()).toEqual(7);

        expect(db.countAbove(stringToByteBuffer(""))).toEqual(7);
        expect(db.countAbove(stringToByteBuffer("A"))).toEqual(6);
        expect(db.countAbove(stringToByteBuffer("B"))).toEqual(3);
        expect(db.countAbove(stringToByteBuffer("BC"))).toEqual(1);
        expect(db.countAbove(stringToByteBuffer("BD"))).toEqual(0);
        expect(db.countAbove(stringToByteBuffer("Z"))).toEqual(0);

        expect(db.countBelow(stringToByteBuffer(""))).toEqual(0);
        expect(db.countBelow(stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBelow(stringToByteBuffer("B"))).toEqual(3);
        expect(db.countBelow(stringToByteBuffer("BD"))).toEqual(6);
        expect(db.countBelow(stringToByteBuffer("ZZZZZ"))).toEqual(7);

        expect(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("ZZZZ"))).toEqual(7);
        expect(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("B"))).toEqual(3);
        expect(db.countBetween(stringToByteBuffer("A"), stringToByteBuffer("B"))).toEqual(2);
        expect(db.countBetween(stringToByteBuffer("B"), stringToByteBuffer("ZZZZ"))).toEqual(3);

        expect(db.countBetween(stringToByteBuffer(""), stringToByteBuffer(""))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer("A"), stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer("AC"), stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer("B"), stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer("BD"), stringToByteBuffer("A"))).toEqual(0);
        expect(db.countBetween(stringToByteBuffer("ZZZ"), stringToByteBuffer("B"))).toEqual(0);

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
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("1"), stringToByteBuffer("one"));
        db.put(stringToByteBuffer("2"), stringToByteBuffer("two"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x3 = new StringBuilder();
        db.getAll((ByteBuffer k, ByteBuffer v) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,<")
                .append(UTF_8.decode(v).toString()).append(">|"));
        expect(x3.toString()).toEqual("<1>,<one>|<2>,<two>|<记!>,<RR>|");

        db.stop();
    }

    @Test
    public void usesGetAllAboveTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x = new StringBuilder();
        db.getAbove(stringToByteBuffer("B"), (ByteBuffer k, ByteBuffer v) -> x.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));

        expect(x.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("B".getBytes());
        db.getAbove(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        db.stop();
    }

    @Test
    public void usesGetAllBelowTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x = new StringBuilder();
        db.getBelow( stringToByteBuffer("AC"), (ByteBuffer k, ByteBuffer v) -> x.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));

        expect(x.toString()).toEqual("A,1|AB,2|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
        keyb.put("\uFFFF".getBytes());
        db.getBelow(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
                .append(UTF_8.decode(v).toString()).append("|"));
        expect(x3.toString()).toEqual("A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

        db.stop();
    }

    @Test
    public void usesGetAllBetweenTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        db.put(stringToByteBuffer("A"), stringToByteBuffer("1"));
        db.put(stringToByteBuffer("AB"), stringToByteBuffer("2"));
        db.put(stringToByteBuffer("AC"), stringToByteBuffer("3"));
        db.put(stringToByteBuffer("B"), stringToByteBuffer("4"));
        db.put(stringToByteBuffer("BB"), stringToByteBuffer("5"));
        db.put(stringToByteBuffer("BC"), stringToByteBuffer("6"));
        db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

        StringBuilder x = new StringBuilder();

        db.getBetween(stringToByteBuffer("A"), stringToByteBuffer("B"), (ByteBuffer k, ByteBuffer v) -> {
                    x.append(UTF_8.decode(k).toString()).append(",").append(UTF_8.decode(v).toString()).append("|");
        });
        expect(x.toString()).toEqual("AB,2|AC,3|");

        StringBuilder x3 = new StringBuilder();
        ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
        key1b.put("B".getBytes());
        ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
        key2b.put("\uFFFF".getBytes());
        db.getBetween(key1b, key2b, (ByteBuffer k, ByteBuffer v) -> {
                     x3.append(UTF_8.decode(k).toString()).append(",").append(UTF_8.decode(v).toString()).append("|");
        });
        expect(x3.toString()).toEqual("BB,5|BC,6|记!,RR|");

        StringBuilder x4 = new StringBuilder();
        db.getBetween(stringToByteBuffer(""),stringToByteBuffer( ""), (ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
        db.getBetween(stringToByteBuffer("A"), stringToByteBuffer("A"), (ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
        db.getBetween(stringToByteBuffer("B"), stringToByteBuffer("A"), (ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
        expect(x4.toString()).toEqual("");

        db.stop();
    }

    @Test
    public void usesBuffersTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

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
        db.getAll((ByteBuffer kb, ByteBuffer vb) -> {
            count.addAndGet(kb.getInt());
            count.addAndGet(vb.getInt());
        });
        expect(count.intValue()).toEqual(12824);

        expect(db.exists(keyb)).toBeTrue();
        expect(db.remove(keyb)).toBeTrue();
        expect(db.exists(keyb)).toBeFalse();
        expect(db.remove(keyb)).toBeFalse();

        db.stop();
    }

    @Test
    public void usesGetBufferIsDirectBufferTest() {
        Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
        // Direct ByteBuffer
        ByteBuffer keybb = ByteBuffer.allocateDirect(16);
        ByteBuffer valbb = ByteBuffer.allocateDirect(16);
        keybb.putInt(42);
        valbb.putInt(42);

        db.put(keybb, valbb);
        db.get(keybb, (ByteBuffer v) -> expect(v.isDirect()).toBeTrue());
        // Indirect ByteBuffer
        byte[] keyb = {41};
        byte[] valb = {41};

        db.put(ByteBuffer.wrap(keyb), ByteBuffer.wrap(valb));
        db.get(ByteBuffer.wrap(keyb), (ByteBuffer v) -> expect(v.isDirect()).toBeTrue());
    }
}
