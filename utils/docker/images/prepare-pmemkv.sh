#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# prepare-pmemkv.sh <package_type> - prepare pmemkv packages
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Merge pull request #612 from igchor/test_refactor; 2.03.2020
current_pmemkv_version="df171fb9bc55c8d3f060ec2dbdeee945a99e7b52"
# stable-1.0: Merge pull request #618 from lukaszstolarczuk/add-hu...; 4.03.2020
stable_1_pmemkv_version="1c15e615dd4ac25aa6fe269c955327b80a4d26dc"
# stable-1.1: Version 1.1; 31.01.2020
stable_1_1_pmemkv_version="2f719305afb0f44103734851cfe825e1b1d73dbf"

prepare_pmemkv () {
	pmemkv_version="$1"
	version_name="$2"
	git checkout "$pmemkv_version"
	mkdir build
	cd build
	cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
		-DCMAKE_INSTALL_PREFIX=$PREFIX \
		-DCPACK_GENERATOR=$PACKAGE_TYPE \
		-DBUILD_TESTS=OFF
	make -j$(nproc) package
	cd ..
	mkdir /opt/"$version_name"
	mv build/* /opt/"$version_name"
	rm -rf build
}

git clone https://github.com/pmem/pmemkv
cd pmemkv

prepare_pmemkv "$current_pmemkv_version" "pmemkv-master"
prepare_pmemkv "$stable_1_pmemkv_version" "pmemkv-stable-1.0"
prepare_pmemkv "$stable_1_1_pmemkv_version" "pmemkv-stable-1.1"

cd ..
rm -r pmemkv
