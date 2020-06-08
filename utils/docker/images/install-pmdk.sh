#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# install-pmdk.sh <package_type> - installs PMDK
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Merge pull request #4625 from kilobyte/rand; 13.03.2020
PMDK_VERSION="ff790ea5fc4c38304a61207346cdee7da381255a"

git clone https://github.com/pmem/pmdk --shallow-since=2020-01-15
cd pmdk
git checkout $PMDK_VERSION

if [ "$PACKAGE_TYPE" = "" ]; then
	make -j$(nproc) install prefix=$PREFIX
else
	make -j$(nproc) BUILD_PACKAGE_CHECK=n $PACKAGE_TYPE
	if [ "$PACKAGE_TYPE" = "dpkg" ]; then
		sudo dpkg -i dpkg/libpmem_*.deb dpkg/libpmem-dev_*.deb
		sudo dpkg -i dpkg/libpmemobj_*.deb dpkg/libpmemobj-dev_*.deb
	elif [ "$PACKAGE_TYPE" = "rpm" ]; then
		sudo rpm -i rpm/*/pmdk-debuginfo-*.rpm
		sudo rpm -i rpm/*/libpmem-*.rpm
		sudo rpm -i rpm/*/libpmemobj-*.rpm
	fi
fi

cd ..
rm -r pmdk
