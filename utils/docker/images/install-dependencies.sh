#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# install-dependencies.sh - install binding's dependencies, so it
#                 can be built offline; and setup maven settings.
#                 Both can be turned off using build arguments (see below if's).
#

set -e

# Include setup of extra maven parameters...
source /opt/setup-maven-settings.sh

# ...and set the same script as an entrypoint for all users (newly defined in the future)
if [ -n "${SKIP_MAVEN_RUNTIME_SETUP}" ]; then
	echo "Variable 'SKIP_MAVEN_RUNTIME_SETUP' is set; skipping building dependencies"
else
	echo "source /opt/setup-maven-settings.sh" >> /etc/skel/.bashrc
fi

if [ -n "${SKIP_DEPENDENCIES_BUILD}" ]; then
	echo "Variable 'SKIP_DEPENDENCIES_BUILD' is set; skipping building dependencies"
	exit
fi

MVN_PARAMS="${PMEMKV_MVN_PARAMS}"
echo "Extra mvn params (taken from env): ${MVN_PARAMS}"

PREFIX=/usr
# common: release 1.4, 15.02.2021
PMEMKV_VERSION="ecb8fd65c5b07ed002d1018418ef809ab50d4e18"
# common: release 1.0.1, 11.03.2021
JAVA_VERSION="827f911c977d475511ca9b29cdec3c12425a3936"

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

mvn install -Dmaven.test.skip=true ${MVN_PARAMS}
mvn javadoc:javadoc ${MVN_PARAMS}
mvn dependency:go-offline ${MVN_PARAMS}
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
