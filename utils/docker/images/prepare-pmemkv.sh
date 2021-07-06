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

# stable-1.0: 1.0.3 release; 06.10.2020
stable_1_pmemkv_version="77ae3ef23dc2b2db9c012dc343b125a785a0ffbc"

# stable-1.1: 1.1 release; 31.01.2020
stable_1_1_pmemkv_version="2f719305afb0f44103734851cfe825e1b1d73dbf"

# stable-1.2: 1.2 release; 29.05.2020
stable_1_2_pmemkv_version="1a9dccfd4b7c7437534838aaec7e5f3e38300dd6"

# stable-1.3: 1.3 release; 02.10.2020
stable_1_3_pmemkv_version="6f79229fd195310f4a45321e86e312e358fe481a"

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
prepare_pmemkv "${stable_1_pmemkv_version}" "pmemkv-stable-1.0"
prepare_pmemkv "${stable_1_1_pmemkv_version}" "pmemkv-stable-1.1"
prepare_pmemkv "${stable_1_2_pmemkv_version}" "pmemkv-stable-1.2"
prepare_pmemkv "${stable_1_3_pmemkv_version}" "pmemkv-stable-1.3"
prepare_pmemkv "${stable_1_4_pmemkv_version}" "pmemkv-stable-1.4"

cd ..
rm -r pmemkv
