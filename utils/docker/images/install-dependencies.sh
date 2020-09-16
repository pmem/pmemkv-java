#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-dependencies.sh - install Java dependencies
#                           so that it can be built offline
#

set -e

PREFIX=/usr

# common: release 1.2, 29.05.2020
PMEMKV_VERSION="1.2"

# common: release 1.0, 30.06.2020
JAVA_VERSION="1.0"

#
# Build and install PMEMKV - JNI will need it
#
git clone https://github.com/pmem/pmemkv.git
cd pmemkv
git checkout $PMEMKV_VERSION
mkdir build
cd build

cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
	-DCMAKE_INSTALL_PREFIX=$PREFIX \
	-DENGINE_CMAP=OFF \
	-DENGINE_CSMAP=OFF \
	-DENGINE_VCMAP=OFF \
	-DENGINE_VSMAP=OFF \
	-DBUILD_DOC=OFF \
	-DBUILD_EXAMPLES=OFF \
	-DBUILD_TESTS=OFF
make -j$(nproc)
make -j$(nproc) install

#
# project's dependencies - all of the dependencies needed to run pmemkv-java will
#                          be saved in the /opt/java directory. It makes building
#                          of this project independent of network connection.
mkdir /opt/java/

deps_dir=$(mktemp -d)
git clone https://github.com/pmem/pmemkv-java.git ${deps_dir}
pushd ${deps_dir}
git checkout $JAVA_VERSION
mvn install
mvn dependency:go-offline
mv -v ~/.m2/repository /opt/java/

popd
rm -r ${deps_dir}

#
# Uninstall all installed stuff
#
cd $WORKDIR/pmemkv/build
make uninstall
# remove any installed pmemkv-java's libs
rm -r /opt/java/repository/io/pmem/*

# make the /opt/java directory world-readable
chmod -R a+r /opt/java
