#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-dependencies.sh - install Java dependencies
#                           so that it can be built offline
#

set -e

# master:  Merge pull request #52 from igchor/builder, 23.06.2020
JAVA_VERSION="155ec92db19769b55c44875ca3beed1e8a62ae7b"

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

# remove any installed pmemkv's libs
rm -r /opt/java/repository/io/pmem/*

popd
rm -r ${deps_dir}

# make the /opt/java directory world-readable
chmod -R a+r /opt/java
