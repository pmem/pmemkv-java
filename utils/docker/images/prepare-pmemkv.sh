#!/usr/bin/env bash
#
# Copyright 2019, Intel Corporation
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in
#       the documentation and/or other materials provided with the
#       distribution.
#
#     * Neither the name of the copyright holder nor the names of its
#       contributors may be used to endorse or promote products derived
#       from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#
# prepare-pmemkv.sh <package_type> - prepare pmemkv packages
#

set -e

package_type=$1

# Merge pull request #510 from lukaszstolarczuk/update-dockerimages-to-newest-versions; 04.11.2019
current_pmemkv_version="780f848c0b4c0d1ee8d6de4107571e8c00e846c5"

# tag: 1.0.1; 28.10.2019
stable_1_pmemkv_version="1.0.1"

prepare_pmemkv () {
	pmemkv_version="$1"
	version_name="$2"
	git checkout "$pmemkv_version"
	mkdir build
	cd build
	cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
		-DCMAKE_INSTALL_PREFIX=$PREFIX \
		-DCPACK_GENERATOR=$package_type \
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

cd ..
rm -r pmemkv
