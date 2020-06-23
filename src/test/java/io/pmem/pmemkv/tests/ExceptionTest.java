package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.DatabaseException;
import io.pmem.pmemkv.Converter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.fail;

class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}

//class ByteBufferConverter implements Converter<ByteBuffer> {
//    public ByteBuffer toByteBuffer(ByteBuffer entry) {
//      return entry;
//    }
//
//    public ByteBuffer fromByteBuffer(ByteBuffer entry) {
//      return entry;
//    }
//}

public class ExceptionTest {

    private final String ENGINE = "vsmap";
    private Database<ByteBuffer, ByteBuffer> db;

    private Database<ByteBuffer, ByteBuffer> buildDB(String engine) {
        return new Database.Builder(engine).
                setSize(1073741824).
                setPath("/dev/shm").
                setKeyConverter(new ByteBufferConverter()).
                setValueConverter(new ByteBufferConverter()).
                <ByteBuffer, ByteBuffer>build();
    }

    @Before
    public void init() {
        db = buildDB(ENGINE);
        // Direct ByteBuffer
        for ( int i = 0; i< 0xFF; i++){
            ByteBuffer key = ByteBuffer.allocateDirect(256);
            key.putInt(i);
            db.put(key, key);
        }
        expect(db.countAll()).toEqual(0xFF);
    }

    @After
    public void finalize(){
        db.stop();
    }

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
                    if( k.getInt() == 15) {
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
}
