# pmemkv-java
Java bindings for pmemkv

The current API is simplified and not functionally equal to its native C/C++ counterpart.
In the future existing API may be extended in idiomatic way without preserving backward compatibility.
All known issues and limitations are logged as GitHub issues.

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

It may be necessary to [configure a proxy](https://maven.apache.org/guides/mini/guide-proxies.html) and set `JAVA_HOME` & `JAVA_TOOL_OPTIONS` environment variables:

```
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
```

Clone the pmemkv-java tree:

```
git clone https://github.com/pmem/pmemkv-java.git
cd pmemkv-java
```

Finish by installing these bindings:

```
LD_LIBRARY_PATH=path_to_your_libs mvn install
```

## Testing

This library includes a set of automated tests that exercise all functionality.

```
LD_LIBRARY_PATH=path_to_your_libs mvn test
```

## Example

We are using `/dev/shm` to
[emulate persistent memory](http://pmem.io/2016/02/22/pm-emulation.html)
in example.

Example can be found within this repository in [examples directory](https://github.com/pmem/pmemkv-java/tree/master/examples).
To execute the example:
```
javac -cp ../target/*.jar BasicExample.java
PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -cp .:`find ../target -name *.jar` -Djava.library.path=/usr/local/lib BasicExample
```

