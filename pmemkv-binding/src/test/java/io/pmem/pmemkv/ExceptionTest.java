// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

package io.pmem.pmemkv;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static io.pmem.pmemkv.TestUtils.*;

@SuppressWarnings("serial")
class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
}

public class ExceptionTest {

	private final String ENGINE = "vsmap";
	private String DB_DIR = "";
	private Database<ByteBuffer, ByteBuffer> db;

	/* Helper method, used in most of the tests in this file */
	private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
		return new Database.Builder<ByteBuffer, ByteBuffer>(engine)
				.setSize(DEFAULT_DB_SIZE)
				.setPath(DB_DIR)
				.setKeyConverter(new ByteBufferConverter())
				.setValueConverter(new ByteBufferConverter())
				.build();
	}

	@Rule
	public TemporaryFolder testDir = new TemporaryFolder(DEFAULT_DB_DIR);

	@Before
	public void init() {
		DB_DIR = testDir.getRoot().toString();
		assertTrue(DB_DIR != null && !DB_DIR.isEmpty());

		db = buildDB(ENGINE);
		// Direct ByteBuffer
		for (int i = 0; i < 0xFF; i++) {
			ByteBuffer key = ByteBuffer.allocateDirect(256);
			key.putInt(i);
			db.put(key, key);
		}
		assertEquals(db.countAll(), 0xFF);
	}

	@After
	public void finalize() {
		db.stop();
	}

	/* Exceptions related to config and in Open method */

	@Test
	public void throwsExceptionOnStartWhenPathIsMissing() {
		Exception exception = assertThrows(InvalidArgumentException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(DEFAULT_DB_SIZE)
					.build();
		});
		assertTrue(exception.getMessage().length() > 0);
	}

	@Test
	public void throwsExceptionOnStartWhenSizeIsMissing() {
		Exception exception = assertThrows(InvalidArgumentException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setPath(DB_DIR)
					.build();
		});
		assertTrue(exception.getMessage().length() > 0);
	}

	@Test
	public void throwsExceptionOnStartWhenEngineIsInvalidTest() {
		Exception exception = assertThrows(WrongEngineNameException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = buildDB("nope.nope");
		});
		assertTrue(exception.getMessage().length() > 0);
	}

	@Test
	public void throwsExceptionOnStartWhenPathIsInvalidTest() {
		Exception exception = assertThrows(DatabaseException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(DEFAULT_DB_SIZE)
					.setPath("/tmp/123/234/345/456/567/678/nope.nope")
					.build();
			/*
			 * It should be InvalidArgumentException, but:
			 * https://github.com/pmem/pmemkv/issues/565
			 */
		});
		assertTrue(exception.getMessage().length() > 0);
	}

	@Test
	public void throwsExceptionOnStartWhenPathIsWrongTypeTest() {
		Exception exception = assertThrows(DatabaseException.class, () -> {
			Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE)
					.setSize(DEFAULT_DB_SIZE)
					.setPath("1234")
					.build();
			/*
			 * It's not a valid path, so it should be InvalidArgumentException, but:
			 * https://github.com/pmem/pmemkv/issues/565
			 */
		});
		assertTrue(exception.getMessage().length() > 0);
	}

	/* Exceptions in Gets methods */

	@Test
	public void exceptionInGetAllTest() {
		String exception_message = "Inner exception";
		Exception exception = assertThrows(CustomException.class, () -> {
			db.getAll((k, v) -> {
				throw new CustomException(exception_message);
			});
		});
		assertEquals(exception_message, exception.getMessage());
	}

	@Test
	public void exceptionInGetKeysTest() {
		String exception_message = "Inner exception";
		AtomicInteger loop_counter = new AtomicInteger(0);
		Exception exception = assertThrows(CustomException.class, () -> {
			db.getKeys((k) -> {
				loop_counter.getAndIncrement();
				throw new CustomException(exception_message);
			});
		});
		assertEquals(exception_message, exception.getMessage());
		assertEquals(loop_counter.intValue(), 1);
	}

	@Test
	public void exceptionInTheMiddleOfGetKeysTest() {
		String exception_message = "Inner exception";
		AtomicInteger loop_counter = new AtomicInteger(0);
		Exception exception = assertThrows(CustomException.class, () -> {
			db.getKeys((k) -> {
				loop_counter.getAndIncrement();
				if (k.getInt() == 15) {
					throw new CustomException(exception_message);
				}
			});
		});
		assertEquals(exception_message, exception.getMessage());
		assertEquals(loop_counter.intValue(), 16);
	}

	@Test
	public void exceptionInGet() {
		ByteBuffer key = ByteBuffer.allocateDirect(256);
		String exception_message = "Lorem ipsum";
		key.putInt(1);
		Exception exception = assertThrows(CustomException.class, () -> {
			db.get(key, (ByteBuffer k) -> {
				throw new CustomException(exception_message);
			});
		});
		assertEquals(exception.getMessage(), exception_message);
	}

	/* Other exceptions */

	@Test(expected = RuntimeException.class)
	public void exceptionsHierarchy() {
		/* All engines should derive from DatabaseException class */
		List<DatabaseException> exceptions = Arrays.asList(new DatabaseException(""), new NotFoundException(""),
				new NotSupportedException(""), new InvalidArgumentException(""), new BuilderException(""),
				new StoppedByCallbackException(""), new OutOfMemoryException(""), new WrongEngineNameException(""),
				new TransactionScopeException(""));

		/* We just make sure DBException is of RuntimeException class */
		throw new DatabaseException("");
	}
}
