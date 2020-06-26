# pmemkv-java

[![Travis build status](https://travis-ci.org/pmem/pmemkv-java.svg?branch=master)](https://travis-ci.org/pmem/pmemkv-java)
[![GHA build status](https://github.com/pmem/pmemkv/workflows/pmemkv-java/badge.svg?branch=master)](https://github.com/pmem/pmemkv-java/actions)
[![PMEMKV-JAVA version](https://img.shields.io/github/tag/pmem/pmemkv-java.svg)](https://github.com/pmem/pmemkv-java/releases/latest)

Java bindings for pmemkv, using Java Native Interface

The current API is simplified and not functionally equal to its native C/C++ counterpart.
In the future existing API may be extended in idiomatic way without preserving backward compatibility.
All known issues and limitations are logged as GitHub issues.

## Dependencies

* [pmemkv](https://github.com/pmem/pmemkv) - native key/value library
* Java 8 or higher
* [Apache Maven](https://maven.apache.org) - build system
* Used only for development & testing:
  * [JUnit](https://junit.org/) - automated test framework
  * [Oleaster Matcher](https://github.com/mscharhag/oleaster/tree/master/oleaster-matcher) - test condition matching library

## Installation

It may be necessary to [configure a proxy](https://maven.apache.org/guides/mini/guide-proxies.html) and set `JAVA_HOME` & `JAVA_TOOL_OPTIONS` environment variables:

```sh
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
```

Clone the pmemkv-java tree:

```sh
git clone https://github.com/pmem/pmemkv-java.git
cd pmemkv-java
```

Build and install Java Native Interface (JNI) and java bindings:

```sh
mvn install
```

If dependencies (pmemkv, libpmemobj-cpp, pmdk, etc.) are installed in non-standard
location it may be also necessary to set it in LD_LIBRARY_PATH, e.g.:

```sh
LD_LIBRARY_PATH=path_to_your_libs mvn install
```

## Testing

This library includes a set of automated tests that exercise all functionality.

```sh
LD_LIBRARY_PATH=path_to_your_libs mvn test
```

## Example

We are using `/dev/shm` to
[emulate persistent memory](https://pmem.io/2016/02/22/pm-emulation.html)
in example.

Example can be found within this repository in [examples directory](https://github.com/pmem/pmemkv-java/tree/master/examples).
To execute the example:

```sh
cd examples
javac -cp ../src/main/target/*.jar BasicExample.java
PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -cp .:`find ../src/main/target -name *.jar` -Djava.library.path=/usr/local/lib BasicExample
```
