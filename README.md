[![GHA build status](https://github.com/pmem/pmemkv-java/workflows/pmemkv-java/badge.svg?branch=master)](https://github.com/pmem/pmemkv-java/actions)
[![PMEMKV-JAVA version](https://img.shields.io/github/tag/pmem/pmemkv-java.svg)](https://github.com/pmem/pmemkv-java/releases/latest)
[![pmemkv-root maven central](https://maven-badges.herokuapp.com/maven-central/io.pmem/pmemkv-root/badge.svg?style=flat-for-the-badge)](https://search.maven.org/artifact/io.pmem/pmemkv-root)

# pmemkv-java

Java bindings for pmemkv, using Java Native Interface. It's mostly functionally equal to pmemkv
in version 1.0, but some of the new functionalities (e.g. from pmemkv 1.4) are already available.

All known issues and limitations are logged as GitHub issues or are described
in pmemkv's man pages.

Java API is documented with javadocs and can be found as html on https://pmem.io/pmemkv-java
for every branch/release. For most recent always see [master](https://pmem.io/pmemkv-java/master/html/index.html) docs.

Latest releases can be found on the ["releases" tab](https://github.com/pmem/pmemkv-java/releases).
Up-to-date support/maintenance status of branches/releases is available on [pmem.io](https://pmem.io/pmemkv-java).

## Dependencies

* [pmemkv 1.4](https://github.com/pmem/pmemkv) - Key-Value Datastore for Persistent Memory
  * pmemkv source package (pmemkv-devel or libpmemkv-dev)
* Java Development Kit 8
* gcc-c++ compiler
* [Apache Maven 3](https://maven.apache.org) - build system

## Usage

### Maven repository

This pmemkv binding is accesible from maven repository:

[io.pmem namespace @ maven.org](https://repo1.maven.org/maven2/io/pmem/pmemkv-root)

You can add our project as a dependency and use it freely. Make sure to use it e.g. like this:

```
<dependency>
  <groupId>io.pmem</groupId>
  <artifactId>pmemkv-root</artifactId>
  <version>[1.0.1,)</version>
</dependency>
```

### Installation

Start by installing [pmemkv](https://github.com/pmem/pmemkv/blob/master/INSTALLING.md)
(currently at least in version **1.4**) in your system. Make sure our helper library `pmemkv_json_config`
is enabled by specifying extra cmake parameter - `cmake .. -DBUILD_JSON_CONFIG=ON ...`.

It may be necessary to [configure a proxy](https://maven.apache.org/guides/mini/guide-proxies.html)
and set `JAVA_HOME` environment variable. Set `JAVA_HOME` variable with directory containing
JDK 8 installed typically in `/usr/lib/jvm/`, command below will set first directory matching to
1.8.0, but path can differ in some exotic distros:

```sh
export JAVA_HOME=`ls -d1 /usr/lib/jvm/* | grep "1.8.0" | head -n 1`
echo $JAVA_HOME
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
location(s) it may be also necessary to set up:
**CPLUS_INCLUDE_PATH** and **LIBRARY_PATH** for compiling and linking JNI code (gcc env. variables),
**LD_LIBRARY_PATH** for examples and tests build/execution.

```sh
CPLUS_INCLUDE_PATH=<path_to_includes> \
LIBRARY_PATH=<path_to_libs> \
LD_LIBRARY_PATH=<path_to_libs> mvn install
```

## Testing

This library includes a set of automated tests that exercise all functionality.

```sh
LD_LIBRARY_PATH=<path_to_libs> mvn test
```

to execute tests on non-default path (`/dev/shm`), setup desired directory, e.g.:

```sh
LD_LIBRARY_PATH=<path_to_libs> mvn test -Dtest.db.dir=/my/test/dir
```

## Examples

We use `/dev/shm` with [emulated persistent memory](https://pmem.io/2016/02/22/pm-emulation.html)
in examples.

Examples can be found within this repository in [examples directory](https://github.com/pmem/pmemkv-java/tree/master/examples).
To execute them, run e.g.:

```sh
cd examples
mvn package
PMEM_IS_PMEM_FORCE=1 java -ea -Xms1G -jar StringExample/target/StringExample-*-jar-with-dependencies.jar
```

If you want to use our examples with pmemkv from maven repository, you can take a look at our
[testing script](./utils/docker/run-maven-example.sh) executed in our [dedicated CI workflow](./.github/workflows/maven.yml).
It boils down to changing build command (`mvn package`) to e.g.:

```sh
mvn package -Dpmemkv.packageName=pmemkv-root -Dpmemkv.packageVersion=1.1.0
```

## Contributing

Any contributions are welcome. Process, hints and good practices
are described in [CONTRIBUTING.md](./CONTRIBUTING.md).

### Debugging

Debugging process is described in [DEBUGGING.md](./DEBUGGING.md).

### Documentation

Docs can be generated using mvn by executing commands:

```sh
mvn javadoc:javadoc
```

## Contact us

For more information about **pmemkv** and java bindings, contact Igor Chorążewicz (igor.chorazewicz@intel.com),
Piotr Balcer (piotr.balcer@intel.com) or post on our **#pmem** Slack channel using
[this invite link](https://join.slack.com/t/pmem-io/shared_invite/enQtNzU4MzQ2Mzk3MDQwLWQ1YThmODVmMGFkZWI0YTdhODg4ODVhODdhYjg3NmE4N2ViZGI5NTRmZTBiNDYyOGJjYTIyNmZjYzQxODcwNDg)
or [Google group](https://groups.google.com/group/pmem).
