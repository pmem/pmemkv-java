---
title: pmemkv-java
layout: main
---

## pmemkv-java

Java bindings for **pmemkv**, using Java Native Interface.

[**pmemkv**](https://pmem.io/pmemkv) is a local/embedded key-value datastore optimized for persistent memory.

Current code of pmemkv-java can be accessed on [github page](https://github.com/pmem/pmemkv-java).

The API of pmemkv-java binding is documented in the following docs:
* [master](./master/html/index.html) - it implements additional API of libpmemkv (and requires min. version of 1.4)
* [v1.1](./v1.1/html/index.html) - it is functionally equivalent to libpmemkv 1.0
* [v1.0](./v1.0/html/index.html) - it is functionally equivalent to libpmemkv 1.0

#### Releases' support status

Currently all branches/releases are fully supported. Latest releases can be
seen on the ["releases" tab on the Github page](https://github.com/pmem/pmemkv-java/releases).

| Version branch | First release date | Last patch release | Maintenance status |
| -------------- | ------------------ | ------------------ | ------------------ |
| stable-1.1 | Jun 08, 2021 | N/A | Full |
| stable-1.0 | Jun 30, 2020 | Mar 12, 2021 | Full |

Possible statuses:
1. Full maintenance:
	* All/most of bugs fixed (if possible),
	* Patch releases issued based on a number of fixes and their severity,
	* At least one release at the end of the maintenance period,
	* Full support for at least a year since the initial release.
2. Limited scope:
	* Only critical bugs (security, data integrity, etc.) will be backported,
	* Patch versions will be released when needed (based on severity of found issues),
	* Branch will remain in "limited maintanance" status based on original release availability in popular distros.
3. EOL:
	* No support,
	* No bug fixes,
	* No official releases.

#### Blog entries

The following blog articles relates to pmemkv-java:
* [API overview of pmemkv-java binding](https://pmem.io/2020/10/30/pmemkv-java-binding.html) - based on v1.0.0 implementation
