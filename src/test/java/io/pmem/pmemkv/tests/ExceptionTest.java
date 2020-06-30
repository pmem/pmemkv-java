// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.fail;

class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
}

public class ExceptionTest {

	private final String ENGINE = "vsmap";
	private Database<ByteBuffer, ByteBuffer> db;

	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		return new Database.Builder<ByteBuffer, ByteBuffer>(engine)
				.setSize(1073741824)
				.setPath("/dev/shm")
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();
	}

	@Before
	public void init() {
		db = buildDB(ENGINE);
		// Direct ByteBuffer
		for (int i = 0; i < 0xFF; i++) {
			ByteBuffer key = ByteBuffer.allocateDirect(256);
			key.putInt(i);
			db.put(key, key);
		}
		expect(db.countAll()).toEqual(0xFF);
	}

	@After
	public void finalize() {
		db.stop();
	}

	/* Exceptions related to config and in Open method */

	@Test
	public void throwsExceptionOnStartWhenPathIsMissing() {
		Database<ByteBuffer, ByteBuffer> db = null;
		boolean exception_occured = false;
		try {
			db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(1073741824)
					.build();
			Assert.fail();
		} catch (InvalidArgumentException kve) {
			exception_occured = true;
		} catch (Exception e) {
			Assert.fail();
		}
		expect(exception_occured).toBeTrue();
		expect(db).toBeNull();
	}

	@Test
	public void throwsExceptionOnStartWhenSizeIsMissing() {
		Database<ByteBuffer, ByteBuffer> db = null;
		boolean exception_occured = false;
		try {
			db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setPath("/dev/shm")
					.build();
			Assert.fail();
		} catch (InvalidArgumentException kve) {
			exception_occured = true;
		} catch (Exception e) {
			Assert.fail();
		}
		expect(db).toBeNull();
		expect(exception_occured).toBeTrue();
	}

	@Test
	public void throwsExceptionOnStartWhenEngineIsInvalidTest() {
		Database<ByteBuffer, ByteBuffer> db = null;
		boolean exception_occured = false;
		try {
			db = buildDB("nope.nope");
			Assert.fail();
		} catch (WrongEngineNameException kve) {
			exception_occured = true;
		} catch (Exception e) {
			Assert.fail();
		}
		expect(db).toBeNull();
		expect(exception_occured).toBeTrue();
	}

	@Test
	public void throwsExceptionOnStartWhenPathIsInvalidTest() {
		Database<ByteBuffer, ByteBuffer> db = null;
		boolean exception_occured = false;
		try {
			db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(1073741824)
					.setPath("/tmp/123/234/345/456/567/678/nope.nope")
					.build();
			Assert.fail();
			/*
			 * It should be InvalidArgumentException, but:
			 * https://github.com/pmem/pmemkv/issues/565
			 */
		} catch (DatabaseException kve) {
			exception_occured = true;
		} catch (Exception e) {
			Assert.fail();
		}
		expect(db).toBeNull();
		expect(exception_occured).toBeTrue();
	}

	@Test
	public void throwsExceptionOnStartWhenPathIsWrongTypeTest() {
		Database<ByteBuffer, ByteBuffer> db = null;
		boolean exception_occured = false;
		try {
			db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(1073741824)
					.setPath("1234")
					.build();
			Assert.fail();
			/*
			 * It's not a valid path, so it should be InvalidArgumentException, but:
			 * https://github.com/pmem/pmemkv/issues/565
			 */
		} catch (DatabaseException kve) {
			exception_occured = true;
		} catch (Exception e) {
			Assert.fail();
		}
		expect(exception_occured).toBeTrue();
		expect(db).toBeNull();
	}

	/* Exceptions in Gets methods */

	@Test
	public void exceptionInGetallTest() {
		int exception_counter = 0;
		try {
			db.getAll((k, v) -> {
				throw new RuntimeException("Inner exception");
			});
		} catch (Exception e) {
			exception_counter++;
		}
		expect(exception_counter).toEqual(1);
	}

	@Test
	public void exceptionInGetKeysTest() {
		int exception_counter = 0;
		AtomicInteger loop_counter = new AtomicInteger(0);
		try {
			db.getKeys((k) -> {
				loop_counter.getAndIncrement();
				throw new RuntimeException("Inner exception");
			});
		} catch (Exception e) {
			exception_counter++;
		}
		expect(exception_counter).toEqual(1);
		expect(loop_counter.intValue()).toEqual(1);
	}

	@Test
	public void exceptionInTheMiddleOfGetKeysTest() {
		int exception_counter = 0;
		AtomicInteger loop_counter = new AtomicInteger(0);
		try {
			db.getKeys((k) -> {
				loop_counter.getAndIncrement();
				if (k.getInt() == 15) {
					throw new RuntimeException("Inner exception");
				}
			});
		} catch (Exception e) {
			exception_counter++;
		}
		expect(exception_counter).toEqual(1);
		expect(loop_counter.intValue()).toEqual(16);
	}

	@Test
	public void exceptionInGet() {
		ByteBuffer key = ByteBuffer.allocateDirect(256);
		key.putInt(1);
		boolean exception_occured = false;
		try {
			db.get(key, (ByteBuffer k) -> {
				throw new CustomException("Lorem ipsum");
			});
		} catch (CustomException e) {
			exception_occured = true;
			expect(e.getMessage()).toEqual("Lorem ipsum");
		}
		expect(exception_occured).toBeTrue();
	}

	/* Other */
	@Test(expected = RuntimeException.class)
	public void exceptionsHierarchy() {
		/* All engines should derive from DatabaseException class */
		List<DatabaseException> exceptions = Arrays.asList(new DatabaseException(""),
				new NotFoundException(""), new NotSupportedException(""), new InvalidArgumentException(""),
				new BuilderException(""), new StoppedByCallbackException(""), new OutOfMemoryException(""),
				new WrongEngineNameException(""), new TransactionScopeException(""));

		/* We just make sure DBException is of RuntimeException class */
		throw new DatabaseException("");
	}
}
