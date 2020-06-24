import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.Converter;

import java.nio.ByteBuffer;

class ByteBufferConverter implements Converter<ByteBuffer> {
    public ByteBuffer toByteBuffer(ByteBuffer entry) {
      return entry;
    }

    public ByteBuffer fromByteBuffer(ByteBuffer entry) {
      return entry;
    }
}

public class ByteBufferExample {

    public static void main(String[] args) {
         String ENGINE = "vsmap";
         String CONFIG = "{\"path\":\"/dev/shm\", \"size\":1073741824}";

        Database<ByteBuffer, ByteBuffer> db = new Database.Builder<ByteBuffer, ByteBuffer>(ENGINE).
                setSize(1073741824).
                setPath("/dev/shm").
                setKeyConverter(new ByteBufferConverter()).
                setValueConverter(new ByteBufferConverter()).
                build();

        // Direct ByteBuffer
        for ( int i = 0; i< 0xFF; i++){
            ByteBuffer key = ByteBuffer.allocateDirect(4);
            key.putInt(i);
            db.put(key, key);
        }
        db.getAll((ByteBuffer k, ByteBuffer v) -> {
            System.out.println("Key: " + String.format("0x%02X", k.getInt()) +
                    " Value: " +  String.format("0x%02X", v.getInt()));
        });

        db.stop();

    }
}
