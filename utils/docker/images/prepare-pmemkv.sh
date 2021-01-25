#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# prepare-pmemkv.sh <package_type> - prepare pmemkv packages
#

set -e

PREFIX=/usr
PACKAGE_TYPE=$1

# master: Merge pull request #878 from lukaszstolarczuk/add-manp...; 20.01.2021
current_pmemkv_version="6ec1c79d319990c579522ea3d37432dc6b15d32c"

# stable-1.0: 1.0.3; 06.10.2020
stable_1_pmemkv_version="77ae3ef23dc2b2db9c012dc343b125a785a0ffbc"

# stable-1.1: Merge pull request #661 from lukaszstolarczuk/fix-g...; 18.11.2020
stable_1_1_pmemkv_version="2f727968c157c2479cf5a878b4c681fd38698a0f"

# stable-1.2: Merge pull request #857 from lukaszstolarczuk/fix-c...; 16.12.2020
stable_1_2_pmemkv_version="877dbdd327b6beae49320d21d2eb04385049ee2b"

# stable-1.3: Merge pull request #864 from pmem/stable-1.2; 16.12.2020
stable_1_3_pmemkv_version="20e10a621ebf4ccd845a0dc5bac36e24f2a31e0e"

if [ "${SKIP_PMEMKV_BUILD}" ]; then
	echo "Variable 'SKIP_PMEMKV_BUILD' is set; skipping building of pmemkv"
	exit
fi

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
prepare_pmemkv "$stable_1_3_pmemkv_version" "pmemkv-stable-1.3"

cd ..
rm -r pmemkv
