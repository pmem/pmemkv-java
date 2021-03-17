// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DatabaseTest {

	private final String ENGINE = "vsmap";

	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		return new Database.Builder<ByteBuffer, ByteBuffer>(engine)
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();
	}

	private static ByteBuffer stringToByteBuffer(String msg) {
		return ByteBuffer.wrap(msg.getBytes());
	}

	private static String byteBufferToString(ByteBuffer buffer) {
		byte[] bytes;
		bytes = new byte[buffer.capacity()];
		buffer.get(bytes);
		return new String(bytes);
	}

	@Test
	public void blackholeTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB("blackhole");
		assertEquals(db.countAll(), 0);
		assertFalse(db.exists(stringToByteBuffer("key1")));
		assertNull(db.getCopy(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertEquals(db.countAll(), 0);
		assertFalse(db.exists(stringToByteBuffer("key1")));
		assertNull(db.getCopy(stringToByteBuffer("key1")));
		assertTrue(db.remove(stringToByteBuffer("key1")));
		assertFalse(db.exists(stringToByteBuffer("key1")));
		assertNull(db.getCopy(stringToByteBuffer("key1")));
		db.stop();
	}

	@Test
	public void startEngineTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertNotNull(db);
		assertFalse(db.stopped());
		db.stop();
		assertTrue(db.stopped());
	}

	@Test
	public void stopsEngineMultipleTimesTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.stopped());
		db.stop();
		assertTrue(db.stopped());
		db.stop();
		assertTrue(db.stopped());
		db.stop();
		assertTrue(db.stopped());
	}

	@Test
	public void getsMissingKeyTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.exists(stringToByteBuffer("key1")));
		assertNull(db.getCopy(stringToByteBuffer("key1")));
		db.stop();
	}

	@Test
	public void putsBasicValueTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		assertFalse(db.exists(stringToByteBuffer("key1")));
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		ByteBuffer resBuff = db.getCopy(stringToByteBuffer("key1"));
		assertEquals(byteBufferToString(resBuff), "value1");
		db.stop();
	}

	@Test
	public void putsEmptyKeyTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		db.put(stringToByteBuffer(""), stringToByteBuffer("empty"));
		db.put(stringToByteBuffer(" "), stringToByteBuffer("single-space"));
		db.put(stringToByteBuffer("\t\t"), stringToByteBuffer("two-tab"));
		assertTrue(db.exists(stringToByteBuffer("")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer(""))), "empty");
		assertTrue(db.exists(stringToByteBuffer(" ")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer(" "))), "single-space");
		assertTrue(db.exists(stringToByteBuffer("\t\t")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("\t\t"))), "two-tab");
		db.stop();
	}

	@Test
	public void putsEmptyValueTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		db.put(stringToByteBuffer("empty"), stringToByteBuffer(""));
		db.put(stringToByteBuffer("single-space"), stringToByteBuffer(" "));
		db.put(stringToByteBuffer("two-tab"), stringToByteBuffer("\t\t"));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("empty"))), "");
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("single-space"))), " ");
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("two-tab"))), "\t\t");
		db.stop();
	}

	@Test
	public void putsMultipleValuesTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		db.put(stringToByteBuffer("key2"), stringToByteBuffer("value2"));
		db.put(stringToByteBuffer("key3"), stringToByteBuffer("value3"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key1"))), "value1");
		assertTrue(db.exists(stringToByteBuffer("key2")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key2"))), "value2");
		assertTrue(db.exists(stringToByteBuffer("key3")));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key3"))), "value3");
		assertEquals(db.countAll(), 3);
		db.stop();
	}

	@Test
	public void putsOverwritingExistingValueTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key1"))), "value1");
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value123"));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key1"))), "value123");
		db.put(stringToByteBuffer("key1"), stringToByteBuffer("asdf"));
		assertEquals(byteBufferToString(db.getCopy(stringToByteBuffer("key1"))), "asdf");
		db.stop();
	}

	@Test
	public void removesKeyandValueTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		db.put(stringToByteBuffer("key1"), stringToByteBuffer("value1"));
		assertTrue(db.exists(stringToByteBuffer("key1")));
		db.get(stringToByteBuffer("key1"), (ByteBuffer v) -> {
			assertEquals(byteBufferToString(v), "value1");
		});
		assertTrue(db.remove(stringToByteBuffer("key1")));
		assertFalse(db.remove(stringToByteBuffer("key1")));
		assertFalse(db.exists(stringToByteBuffer("key1")));
		assertNull(db.getCopy(stringToByteBuffer("key1")));

		db.stop();
	}

	@Test
	public void usesGetKeysTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);
		db.put(stringToByteBuffer("1"), stringToByteBuffer("one"));
		db.put(stringToByteBuffer("2"), stringToByteBuffer("two"));
		db.put(stringToByteBuffer("记!"), stringToByteBuffer("RR"));

		assertEquals(db.countAll(), 3);
		StringBuilder x3 = new StringBuilder();
		db.getKeys((ByteBuffer k) -> x3.append("<").append(UTF_8.decode(k).toString()).append(">,"));
		assertEquals(x3.toString(), "<1>,<2>,<记!>,");

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
		assertEquals(x3.toString(), "BB,BC,记!,");

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
		assertEquals(x3.toString(), "A,AB,AC,B,BB,BC,记!,");

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
		assertEquals(x3.toString(), "BB,BC,记!,");

		StringBuilder x4 = new StringBuilder();
		db.getKeysBetween(stringToByteBuffer(""), stringToByteBuffer(""), (ByteBuffer k) -> x4.append(k).append(","));
		db.getKeysBetween(stringToByteBuffer("A"), stringToByteBuffer("A"), (ByteBuffer k) -> x4.append(k).append(","));
		db.getKeysBetween(stringToByteBuffer("B"), stringToByteBuffer("A"), (ByteBuffer k) -> x4.append(k).append(","));
		assertEquals(x4.toString(), "");

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
		assertEquals(db.countAll(), 7);

		assertEquals(db.countAbove(stringToByteBuffer("")), 7);
		assertEquals(db.countAbove(stringToByteBuffer("A")), 6);
		assertEquals(db.countAbove(stringToByteBuffer("B")), 3);
		assertEquals(db.countAbove(stringToByteBuffer("BC")), 1);
		assertEquals(db.countAbove(stringToByteBuffer("BD")), 0);
		assertEquals(db.countAbove(stringToByteBuffer("Z")), 0);

		assertEquals(db.countBelow(stringToByteBuffer("")), 0);
		assertEquals(db.countBelow(stringToByteBuffer("A")), 0);
		assertEquals(db.countBelow(stringToByteBuffer("B")), 3);
		assertEquals(db.countBelow(stringToByteBuffer("BD")), 6);
		assertEquals(db.countBelow(stringToByteBuffer("ZZZZZ")), 7);

		assertEquals(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("ZZZZ")), 7);
		assertEquals(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("A")), 0);
		assertEquals(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("B")), 3);
		assertEquals(db.countBetween(stringToByteBuffer("A"), stringToByteBuffer("B")), 2);
		assertEquals(db.countBetween(stringToByteBuffer("B"), stringToByteBuffer("ZZZZ")), 3);

		assertEquals(db.countBetween(stringToByteBuffer(""), stringToByteBuffer("")), 0);
		assertEquals(db.countBetween(stringToByteBuffer("A"), stringToByteBuffer("A")), 0);
		assertEquals(db.countBetween(stringToByteBuffer("AC"), stringToByteBuffer("A")), 0);
		assertEquals(db.countBetween(stringToByteBuffer("B"), stringToByteBuffer("A")), 0);
		assertEquals(db.countBetween(stringToByteBuffer("BD"), stringToByteBuffer("A")), 0);
		assertEquals(db.countBetween(stringToByteBuffer("ZZZ"), stringToByteBuffer("B")), 0);

		ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
		key1b.put("B".getBytes());
		ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
		key2b.put("BD".getBytes());
		assertEquals(db.countAbove(key1b), 3);
		assertEquals(db.countBelow(key2b), 6);
		assertEquals(db.countBetween(key1b, key2b), 2);

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
		assertEquals(x3.toString(), "<1>,<one>|<2>,<two>|<记!>,<RR>|");

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
		db.getAbove(stringToByteBuffer("B"), (ByteBuffer k, ByteBuffer v) -> x.append(UTF_8.decode(k).toString())
				.append(",").append(UTF_8.decode(v).toString()).append("|"));

		assertEquals(x.toString(), "BB,5|BC,6|记!,RR|");

		StringBuilder x3 = new StringBuilder();
		ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
		keyb.put("B".getBytes());
		db.getAbove(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
				.append(UTF_8.decode(v).toString()).append("|"));
		assertEquals(x3.toString(), "BB,5|BC,6|记!,RR|");

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
		db.getBelow(stringToByteBuffer("AC"), (ByteBuffer k, ByteBuffer v) -> x.append(UTF_8.decode(k).toString())
				.append(",").append(UTF_8.decode(v).toString()).append("|"));

		assertEquals(x.toString(), "A,1|AB,2|");

		StringBuilder x3 = new StringBuilder();
		ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
		keyb.put("\uFFFF".getBytes());
		db.getBelow(keyb, (ByteBuffer k, ByteBuffer v) -> x3.append(UTF_8.decode(k).toString()).append(",")
				.append(UTF_8.decode(v).toString()).append("|"));
		assertEquals(x3.toString(), "A,1|AB,2|AC,3|B,4|BB,5|BC,6|记!,RR|");

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
		assertEquals(x.toString(), "AB,2|AC,3|");

		StringBuilder x3 = new StringBuilder();
		ByteBuffer key1b = ByteBuffer.allocateDirect(1000);
		key1b.put("B".getBytes());
		ByteBuffer key2b = ByteBuffer.allocateDirect(1000);
		key2b.put("\uFFFF".getBytes());
		db.getBetween(key1b, key2b, (ByteBuffer k, ByteBuffer v) -> {
			x3.append(UTF_8.decode(k).toString()).append(",").append(UTF_8.decode(v).toString()).append("|");
		});
		assertEquals(x3.toString(), "BB,5|BC,6|记!,RR|");

		StringBuilder x4 = new StringBuilder();
		db.getBetween(stringToByteBuffer(""), stringToByteBuffer(""),
				(ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
		db.getBetween(stringToByteBuffer("A"), stringToByteBuffer("A"),
				(ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
		db.getBetween(stringToByteBuffer("B"), stringToByteBuffer("A"),
				(ByteBuffer k, ByteBuffer v) -> x4.append(k).append(","));
		assertEquals(x4.toString(), "");

		db.stop();
	}

	@Test
	public void usesBuffersTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB(ENGINE);

		ByteBuffer keyb = ByteBuffer.allocateDirect(1000);
		ByteBuffer valb = ByteBuffer.allocateDirect(1000);
		keyb.putInt(123);
		valb.putInt(234);
		assertFalse(db.exists(keyb));
		db.put(keyb, valb);
		assertTrue(db.exists(keyb));
		assertEquals(db.countAll(), 1);

		keyb.clear();
		keyb.putInt(5678);
		valb.clear();
		valb.putInt(6789);
		assertFalse(db.exists(keyb));
		db.put(keyb, valb);
		assertTrue(db.exists(keyb));
		assertEquals(db.countAll(), 2);

		AtomicInteger count = new AtomicInteger(0);
		db.getAll((ByteBuffer kb, ByteBuffer vb) -> {
			count.addAndGet(kb.getInt());
			count.addAndGet(vb.getInt());
		});
		assertEquals(count.intValue(), 12824);

		assertTrue(db.exists(keyb));
		assertTrue(db.remove(keyb));
		assertFalse(db.exists(keyb));
		assertFalse(db.remove(keyb));

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
		db.get(keybb, (ByteBuffer v) -> assertTrue(v.isDirect()));
		// Indirect ByteBuffer
		byte[] keyb = {41};
		byte[] valb = {41};

		db.put(ByteBuffer.wrap(keyb), ByteBuffer.wrap(valb));
		db.get(ByteBuffer.wrap(keyb), (ByteBuffer v) -> assertTrue(v.isDirect()));
	}

	@Test
	public void usesGetMultiThreadedTest() {
		Database<ByteBuffer, ByteBuffer> db = buildDB("vcmap");

		for (int i = 0; i < 100; ++i) {
			db.put(stringToByteBuffer(Integer.toString(i)), stringToByteBuffer(Integer.toString(i + 1)));
		}

		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < 8; ++i) {
			threads.add(new Thread() {
				public void run() {
					for (int j = 0; j < 100; ++j) {
						final int x = j;
						db.get(stringToByteBuffer(Integer.toString(x)), (ByteBuffer v) -> {
							assertEquals(byteBufferToString(v), Integer.toString(x + 1));
						});
					}
				}
			});
		}

		for (int i = 0; i < 8; ++i) {
			threads.add(new Thread() {
				public void run() {
					for (int j = 99; j >= 0; --j) {
						final int x = j;
						db.get(stringToByteBuffer(Integer.toString(x)), (ByteBuffer v) -> {
							assertEquals(byteBufferToString(v), Integer.toString(x + 1));
						});
					}
				}
			});
		}

		for (Thread t : threads) {
			t.run();
		}

		for (Thread t : threads) {
			try {
				t.join();
			} catch (Exception e) {
				assertTrue(false);
			}
		}

		db.stop();
	}

	@Test
	public void usesGetAllMultiThreadedTest() {
		/* XXX */
	}

	@Test
	public void usesNotDefaultCacheBuffersTest() {
		Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>("vcmap")
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.setKeyBufferSize(5)
				.setValueBufferSize(5)
				.build();

		/* cache buffers should be used */
		db.put(stringToByteBuffer("A"), stringToByteBuffer("A"));
		db.get(stringToByteBuffer("A"), (ByteBuffer v) -> {
			assertEquals(byteBufferToString(v), "A");
		});

		/* key size > key cache buffer size */
		db.put(stringToByteBuffer("AAAAAAAAAAAAAAAAA"), stringToByteBuffer("A"));
		db.get(stringToByteBuffer("AAAAAAAAAAAAAAAAA"), (ByteBuffer v) -> {
			assertEquals(byteBufferToString(v), "A");
		});

		/* value size > value cache buffer size */
		db.put(stringToByteBuffer("B"), stringToByteBuffer("AAAAAAAAAAAAAAAAA"));
		db.get(stringToByteBuffer("B"), (ByteBuffer v) -> {
			assertEquals(byteBufferToString(v), "AAAAAAAAAAAAAAAAA");
		});

		/* both sizes are bigger than their cache buffers */
		db.put(stringToByteBuffer("BBBBBBBBBBBBBBBBB"), stringToByteBuffer("BBBBBBBBBBBBBBBBB"));
		db.get(stringToByteBuffer("BBBBBBBBBBBBBBBBB"), (ByteBuffer v) -> {
			assertEquals(byteBufferToString(v), "BBBBBBBBBBBBBBBBB");
		});

		db.stop();
	}
}
