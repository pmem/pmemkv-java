package io.pmem.pmemkv.tests;

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.DatabaseException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.fail;


public class ExceptionTest {

    private final String ENGINE = "vsmap";
    private final String CONFIG = "{\"path\":\"/dev/shm\", \"size\":1073741824}";
    private Database db;

    @Before
    public void init() {
        db = new Database(ENGINE, CONFIG);
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
            db.getAll((ByteBuffer k, ByteBuffer v) -> {
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
                db.getKeys((ByteBuffer k) -> {
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
                db.getKeys((ByteBuffer k) -> {
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
}
