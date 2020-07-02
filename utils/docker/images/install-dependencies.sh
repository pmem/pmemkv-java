#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-dependencies.sh - install Java dependencies
#                           so that it can be built offline
#

set -e

# common: release 1.0, 30.06.2020
JAVA_VERSION="bada69f43447d7a664171458e0ca6d5d535feeb3"

#
# project's dependencies - all of the dependencies needed to run pmemkv-java will
#                          be saved in the /opt/java directory. It makes building
#                          of this project independent of network connection.
mkdir /opt/java/

deps_dir=$(mktemp -d)
git clone https://github.com/pmem/pmemkv-java.git ${deps_dir}
pushd ${deps_dir}
git checkout $JAVA_VERSION
mvn install -Dmaven.test.skip=true
mvn dependency:go-offline
mv -v ~/.m2/repository /opt/java/

# remove any installed pmemkv's libs
rm -r /opt/java/repository/io/pmem/*

popd
rm -r ${deps_dir}

# make the /opt/java directory world-readable
chmod -R a+r /opt/java
