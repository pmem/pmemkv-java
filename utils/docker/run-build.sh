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
# run-build.sh - checks bindings' building and installation
#                with given version of pmemkv
#

PREFIX=/usr

set -e

case $1 in
	master)
		PMEMKV_VERSION="1.0.1-rc1"
		;;
	stable-1.0)
		PMEMKV_VERSION="stable-1.0"
		;;
	*)
		echo "Error: incorrect version of pmemkv: $1 (available: master, stable-1.0)"
		exit 1
		;;
esac

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

# build and install pmemkv
cd ~
git clone https://github.com/pmem/pmemkv.git
cd pmemkv
git checkout $PMEMKV_VERSION
mkdir build
cd build
cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo \
	-DCMAKE_INSTALL_PREFIX=$PREFIX
make -j2
echo $USERPASS | sudo -S make install

echo
echo "###########################################################"
echo "### Verifying building and installing of the java bindings "
echo "###########################################################"
cd ~
git clone https://github.com/pmem/pmemkv-java.git
cd pmemkv-java
mvn install

echo
echo "################################################################"
echo "### Executing test(s) from pmemkv-tools"
echo "################################################################"
cd ~
git clone https://github.com/pmem/pmemkv-tools.git
cd pmemkv-tools
make example_java
