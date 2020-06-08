#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-dependencies.sh - install Java dependencies
#                           so that it can be built offline
#

set -e

# master: Merge pull request #37 from lukaszstolarczuk/set-new-j..., 06.12.2019
JAVA_VERSION="49c0fbe4f8727b279c7aa073963792471bb5dbe7"

#
# project's dependencies - all of the dependencies needed to run pmemkv-java will
#                          be saved in the /opt/java directory. It makes building
#                          of this project independent of network connection.
mkdir /opt/java/

deps_dir=$(mktemp -d)
git clone https://github.com/pmem/pmemkv-java.git ${deps_dir}
pushd ${deps_dir}
git checkout $JAVA_VERSION
mvn dependency:go-offline
mvn install -Dmaven.test.skip=true
mv -v ~/.m2/repository /opt/java/
popd
rm -r ${deps_dir}

# make the /opt/java directory world-readable
chmod -R a+r /opt/java
