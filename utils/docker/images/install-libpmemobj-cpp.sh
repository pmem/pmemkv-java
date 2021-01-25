#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# install-libpmemobj-cpp.sh <package_type>
#			- installs PMDK C++ bindings (libpmemobj-cpp)
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Merge pull request #991 from igchor/merge_1.11_into_master; 10.12.2020
LIBPMEMOBJ_CPP_VERSION="1277d53c15a0e69ba7af2912818c72f2cb7ac708"

if [ "${SKIP_LIBPMEMOBJ_CPP_BUILD}" ]; then
	echo "Variable 'SKIP_LIBPMEMOBJ_CPP_BUILD' is set; skipping building of libpmemobj-cpp"
	exit
fi

git clone https://github.com/pmem/libpmemobj-cpp --shallow-since=2020-04-28
cd libpmemobj-cpp
git checkout $LIBPMEMOBJ_CPP_VERSION

mkdir build
cd build

cmake .. -DCPACK_GENERATOR="$PACKAGE_TYPE" \
	-DCMAKE_INSTALL_PREFIX=$PREFIX \
	-DCMAKE_BUILD_TYPE=RelWithDebInfo

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
