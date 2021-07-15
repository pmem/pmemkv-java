#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# prepare-pmemkv.sh <package_type> - prepare pmemkv packages
#

set -e

if [ "${SKIP_PMEMKV_BUILD}" ]; then
	echo "Variable 'SKIP_PMEMKV_BUILD' is set; skipping building of pmemkv"
	exit
fi

PREFIX=/usr
PACKAGE_TYPE=${1}
echo "PACKAGE_TYPE: ${PACKAGE_TYPE}"
if [ -z "${PACKAGE_TYPE}" ]; then
	echo "PACKAGE_TYPE is not set"
	exit 1
fi

# master: Merge pull request #991 from lukaszstolarczuk/update-dockers; 05.07.2021
current_pmemkv_version="fd0f0f20989b1d8c75f54f3151542475a7da37d0"

# stable-1.4: 1.4 release; 15.02.2021
stable_1_4_pmemkv_version="ecb8fd65c5b07ed002d1018418ef809ab50d4e18"

prepare_pmemkv () {
	pmemkv_version="${1}"
	version_name="${2}"
	git checkout "${pmemkv_version}"
	mkdir /opt/"${version_name}"
	mkdir build
	cd build
	# turn off all redundant components
	cmake .. -DCPACK_GENERATOR="${PACKAGE_TYPE}" -DCMAKE_INSTALL_PREFIX=${PREFIX} \
		-DBUILD_EXAMPLES=OFF -DBUILD_TESTS=OFF -DTESTS_USE_VALGRIND=OFF -DBUILD_DOC=OFF
	make -j$(nproc) package
	mv * /opt/"${version_name}"
	cd ..
	rm -rf build
}

git clone https://github.com/pmem/pmemkv
cd pmemkv

prepare_pmemkv "${current_pmemkv_version}" "pmemkv-master"
prepare_pmemkv "${stable_1_4_pmemkv_version}" "pmemkv-stable-1.4"

cd ..
rm -r pmemkv
