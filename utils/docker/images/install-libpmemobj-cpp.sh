#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# install-libpmemobj-cpp.sh [package_type]
#			- installs PMDK C++ bindings (libpmemobj-cpp)
#

set -e

if [ "${SKIP_LIBPMEMOBJ_CPP_BUILD}" ]; then
	echo "Variable 'SKIP_LIBPMEMOBJ_CPP_BUILD' is set; skipping building of libpmemobj-cpp"
	exit
fi

PREFIX=/usr
PACKAGE_TYPE=${1^^} #To uppercase
echo "PACKAGE_TYPE: ${PACKAGE_TYPE}"

# master: Merge pull request #1126 from igchor/test_workaround; 05.07.2021
LIBPMEMOBJ_CPP_VERSION="2543afae7b1e9e82809a3e4280753b33cdab8136"

build_dir=$(mktemp -d -t libpmemobj-cpp-XXX)

git clone https://github.com/pmem/libpmemobj-cpp --shallow-since=2020-06-01 ${build_dir}

pushd ${build_dir}
git checkout ${LIBPMEMOBJ_CPP_VERSION}

mkdir build
cd build

# turn off all redundant components
cmake .. -DCPACK_GENERATOR="${PACKAGE_TYPE}" -DCMAKE_INSTALL_PREFIX=${PREFIX} \
	-DBUILD_EXAMPLES=OFF -DBUILD_TESTS=OFF -DTESTS_USE_VALGRIND=OFF -DBUILD_DOC=OFF -DBUILD_BENCHMARKS=OFF

if [ "${PACKAGE_TYPE}" = "" ]; then
	make -j$(nproc) install
else
	make -j$(nproc) package
	if [ "${PACKAGE_TYPE}" = "DEB" ]; then
		sudo dpkg -i libpmemobj++*.deb
	elif [ "${PACKAGE_TYPE}" = "RPM" ]; then
		sudo rpm -i libpmemobj++*.rpm
	fi
fi

popd
rm -r ${build_dir}
