import io.pmem.pmemkv.Database;

public class BasicExample {
    public static void main(String[] args) {
        System.out.println("Starting engine");
        Database db = new Database("vsmap", "{\"path\":\"/dev/shm\", \"size\":1073741824}");

        System.out.println("Putting new key");
        db.put("key1", "value1");
        assert db.countAll() == 1;

        System.out.println("Reading key back");
        assert db.getCopy("key1").equals("value1");

        System.out.println("Iterating existing keys");
        db.put("key2", "value2");
        db.put("key3", "value3");
        db.getKeys((String k) -> System.out.println("  visited: " + k));

        System.out.println("Removing existing key");
        db.remove("key1");
        assert !db.exists("key1");

        System.out.println("Stopping engine");
        db.stop();
    }
}
