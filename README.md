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

It may be necessary to [configure a proxy](https://maven.apache.org/guides/mini/guide-proxies.html) and set `JAVA_HOME`, `JAVA_TOOL_OPTIONS`:

```
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
```

Finish by installing these bindings:

```
LD_LIBRARY_PATH=/usr/local/lib/:/opt/tbb/lib/intel64/gcc4.7/ mvn install
```

## Testing

This library includes a set of automated tests that exercise all functionality.

```
LD_LIBRARY_PATH=/usr/local/lib/:/opt/tbb/lib/intel64/gcc4.7/ mvn test
```

## Example

We are using `/dev/shm` to
[emulate persistent memory](http://pmem.io/2016/02/22/pm-emulation.html)
in this simple example.

```java
import io.pmem.pmemkv.KVEngine;

public class Example {
    public static void main(String[] args) {
        System.out.println("Starting engine");
        KVEngine kv = new KVEngine("vsmap", "{\"path\":\"/dev/shm/\"}");

        System.out.println("Putting new key");
        kv.put("key1", "value1");
        assert kv.count() == 1;

        System.out.println("Reading key back");
        assert kv.get("key1").equals("value1");

        System.out.println("Iterating existing keys");
        kv.put("key2", "value2");
        kv.put("key3", "value3");
        kv.all((String k) -> System.out.println("  visited: " + k));

        System.out.println("Removing existing key");
        kv.remove("key1");
        assert !kv.exists("key1");

        System.out.println("Stopping engine");
        kv.stop();
    }
}
```
