#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# prepare-pmemkv.sh <package_type> - prepare pmemkv packages
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Remove VERSION file; 29.05.2020
current_pmemkv_version="244c3b0c45f5700f6880fa9dbb0fbc41d7857a33"

# stable-1.0: Merge pull request #686 from lukaszstolarczuk/fix-m...; 20.05.2020
stable_1_pmemkv_version="5a11aee01b2206e519004baae1d26a41d328762c"

# stable-1.1: Merge pull request #661 from lukaszstolarczuk/fix-g...; 17.04.2020
stable_1_1_pmemkv_version="d4de10fa09d0ce99eb15d53f7311f1b4e7c56d47"

# stable-1.2: Version 1.2; 29.05.2020
stable_1_2_pmemkv_version="1.2"

prepare_pmemkv () {
	pmemkv_version="$1"
	version_name="$2"
	git checkout "$pmemkv_version"
	mkdir /opt/"$version_name"
	mkdir build
	cd build
	cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
		-DCMAKE_INSTALL_PREFIX=$PREFIX \
		-DCPACK_GENERATOR=$PACKAGE_TYPE \
		-DBUILD_TESTS=OFF
	make -j$(nproc) package
	mv * /opt/"$version_name"
	cd ..
	rm -rf build
}

git clone https://github.com/pmem/pmemkv
cd pmemkv

prepare_pmemkv "$current_pmemkv_version" "pmemkv-master"
prepare_pmemkv "$stable_1_pmemkv_version" "pmemkv-stable-1.0"
prepare_pmemkv "$stable_1_1_pmemkv_version" "pmemkv-stable-1.1"
prepare_pmemkv "$stable_1_2_pmemkv_version" "pmemkv-stable-1.2"

cd ..
rm -r pmemkv
