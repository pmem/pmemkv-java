#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# install-dependencies.sh - install binding's dependencies
#                           so it can be built offline
#

set -e

PREFIX=/usr

# common: release 1.2, 29.05.2020
PMEMKV_VERSION="1.2"

# common: release 1.0, 30.06.2020
JAVA_VERSION="1.0"

if [ "${SKIP_PMDK_BUILD}" ]; then
	echo "Variable 'SKIP_DEPENDENCIES_BUILD' is set; skipping building dependencies"
	exit
fi

echo "Build and install PMEMKV (JNI needs it)"
git clone https://github.com/pmem/pmemkv.git
cd pmemkv
git checkout ${PMEMKV_VERSION}
mkdir build
cd build

cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
	-DCMAKE_INSTALL_PREFIX=${PREFIX} \
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
echo "Save binding's dependencies in /opt/java"
mkdir /opt/java/

deps_dir=$(mktemp -d)
git clone https://github.com/pmem/pmemkv-java.git ${deps_dir}
pushd ${deps_dir}
git checkout ${JAVA_VERSION}
mvn install -Dmaven.test.skip=true
mvn javadoc:javadoc
mvn dependency:go-offline
mv -v ~/.m2/repository /opt/java/

popd
rm -r ${deps_dir}

echo "Uninstall pmemkv"
cd ${WORKDIR}/pmemkv/build
make uninstall

echo "Remove installed binding files (from local mvn repository)"
rm -r /opt/java/repository/io/pmem/*

echo "Make the /opt/java directory world-readable"
chmod -R a+r /opt/java
