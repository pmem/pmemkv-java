# pmemkv-java
Java bindings for pmemkv

*This is experimental pre-release software and should not be used in
production systems. APIs and file formats may change at any time without
preserving backwards compatibility. All known issues and limitations
are logged as GitHub issues.*

## Dependencies

* Java 8 or higher
* [PMDK](https://github.com/pmem/pmdk) - native persistent memory libraries
* [pmemkv](https://github.com/pmem/pmemkv) - native key/value library
* [pmemkv-jni](https://github.com/pmem/pmemkv-jni) - JNI integration library
* Used only for development & testing:
  * [Apache Maven](https://maven.apache.org) - build system
  * [JUnit](http://junit.org/) - automated test framework
  * [Oleaster Matcher](https://github.com/mscharhag/oleaster/tree/master/oleaster-matcher) - test condition matching library

## Installation

Start by installing [pmemkv](https://github.com/pmem/pmemkv/blob/master/INSTALLING.md) on your system.

Next install [pmemkv-jni](https://github.com/pmem/pmemkv-jni).

Finish by installing these bindings:  `mvn install`

## Example

We are using `/dev/shm` to
[emulate persistent memory](http://pmem.io/2016/02/22/pm-emulation.html)
in this simple example.

```java
import io.pmem.pmemkv.KVEngine;

public class Example {
    public static void main(String[] args) {
        System.out.println("Opening datastore");
        KVEngine kv = new KVEngine("kvtree3", "/dev/shm/pmemkv", 1073741824); // 1 GB pool

        System.out.println("Putting new key");
        kv.put("key1", "value1");
        assert kv.count() == 1;

        System.out.println("Reading key back");
        assert kv.get("key1").equals("value1");

        System.out.println("Iterating existing keys");
        kv.put("key2", "value2");
        kv.put("key3", "value3");
        kv.allStrings((k) -> System.out.println("  visited: " + k));

        System.out.println("Removing existing key");
        kv.remove("key1");
        assert !kv.exists("key1");

        System.out.println("Closing datastore");
        kv.close();
    }
}
```
