#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# install-pmdk.sh [package_type] [install_prefix] - installs PMDK
#

set -e

if [ "${SKIP_PMDK_BUILD}" ]; then
	echo "Variable 'SKIP_PMDK_BUILD' is set; skipping building of PMDK"
	exit
fi

PACKAGE_TYPE=$1
PREFIX=${2:-/usr}
echo "PACKAGE_TYPE: ${PACKAGE_TYPE}"

# master: 1.11.0, 02.07.2021
PMDK_VERSION="8583fcfd68764ac6779e6f93db89b06971b26704"

git clone https://github.com/pmem/pmdk --shallow-since=2020-12-01
cd pmdk
git checkout ${PMDK_VERSION}

if [ "${PACKAGE_TYPE}" = "" ]; then
	make -j$(nproc) install prefix=${PREFIX}
else
	make -j$(nproc) BUILD_PACKAGE_CHECK=n ${PACKAGE_TYPE}
	if [ "${PACKAGE_TYPE}" = "dpkg" ]; then
		sudo dpkg -i dpkg/libpmem_*.deb dpkg/libpmem-dev_*.deb
		sudo dpkg -i dpkg/libpmemobj_*.deb dpkg/libpmemobj-dev_*.deb
	elif [ "${PACKAGE_TYPE}" = "rpm" ]; then
		sudo rpm -i rpm/*/pmdk-debuginfo-*.rpm
		sudo rpm -i rpm/*/libpmem-*.rpm
		sudo rpm -i rpm/*/libpmemobj-*.rpm
	fi
fi

cd ..
rm -r pmdk
