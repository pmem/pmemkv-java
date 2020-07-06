[![Travis build status](https://travis-ci.org/pmem/pmemkv-java.svg?branch=master)](https://travis-ci.org/pmem/pmemkv-java)
[![GHA build status](https://github.com/pmem/pmemkv-java/workflows/pmemkv-java/badge.svg?branch=master)](https://github.com/pmem/pmemkv-java/actions)
[![PMEMKV-JAVA version](https://img.shields.io/github/tag/pmem/pmemkv-java.svg)](https://github.com/pmem/pmemkv-java/releases/latest)

# pmemkv-java

Java bindings for pmemkv, using Java Native Interface. Currently functionally equal to pmemkv in version 1.0.
Some of the new functionalities (from pmemkv 1.1+) are not available yet.

All known issues and limitations are logged as GitHub issues or are described
in pmemkv's man pages.

Java API is documented with javadocs and can be found as html here:

- [master](https://pmem.io/pmemkv-java/master/html/index.html)
- [v1.0](https://pmem.io/pmemkv-java/v1.0/html/index.html)

## Dependencies

* [pmemkv](https://github.com/pmem/pmemkv) - native key/value library
  * pmemkv source package (libpmemkv-devel, pmemkv-dev)
* Java 8 or higher
* gcc-c++ compiler
* [Apache Maven](https://maven.apache.org) - build system

## Installation

Start by installing [pmemkv](https://github.com/pmem/pmemkv/blob/master/INSTALLING.md)
(currently at least in version **1.0.2**) in your system.

It may be necessary to [configure a proxy](https://maven.apache.org/guides/mini/guide-proxies.html)
and set `JAVA_HOME` environment variable:

```sh
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
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

## Examples

We are using `/dev/shm` to
[emulate persistent memory](https://pmem.io/2016/02/22/pm-emulation.html)
in examples.

Examples can be found within this repository in [examples directory](https://github.com/pmem/pmemkv-java/tree/master/examples).
To execute them, run e.g.:

```sh
cd examples
javac -cp ../src/main/target/*.jar StringExample.java
PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -cp .:`find ../src/main/target -name *.jar` -Djava.library.path=../src/main/cpp/target StringExample
```

## Documentation

Docs can be generated using mvn by executing commands:

```sh
mvn javadoc:javadoc
```
