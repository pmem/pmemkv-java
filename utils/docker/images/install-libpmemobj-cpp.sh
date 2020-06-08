#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-libpmemobj-cpp.sh <package_type>
#			- installs PMDK C++ bindings (libpmemobj-cpp)
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Merge pull request #690 from pmem/stable-1.9; 12.03.2020
LIBPMEMOBJ_CPP_VERSION="13099c702a9fe6aef97af3ffbc0530c60971948c"

git clone https://github.com/pmem/libpmemobj-cpp --shallow-since=2020-01-15
cd libpmemobj-cpp
git checkout $LIBPMEMOBJ_CPP_VERSION

mkdir build
cd build

cmake .. -DCPACK_GENERATOR="$PACKAGE_TYPE" -DCMAKE_INSTALL_PREFIX=$PREFIX

if [ "$PACKAGE_TYPE" = "" ]; then
	make -j$(nproc) install
else
	make -j$(nproc) package
	if [ "$PACKAGE_TYPE" = "DEB" ]; then
		sudo dpkg -i libpmemobj++*.deb
	elif [ "$PACKAGE_TYPE" = "RPM" ]; then
		sudo rpm -i libpmemobj++*.rpm
	fi
fi

cd ../..
rm -r libpmemobj-cpp
