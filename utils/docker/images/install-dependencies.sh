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
# install-dependencies.sh - install Java dependencies
#                           so that it can be built offline
#

set -e

# package manager: DEB or RPM
PACKAGE_MANAGER=$1

# Merge pull request #30 from ldorau/Fixes-after-review, 30.10.2019
JNI_VERSION="41499c7f3bdc16459bf73d458049f81084d64001"

# add ChangeLog 0.9, 4.10.2019
JAVA_VERSION="0.9"

PREFIX=/usr

WORKDIR=$(pwd)

#
# 1) Install PMEMKV
#
cd /opt/pmemkv-stable-1.0/
if [ "${PACKAGE_MANAGER}" = "DEB" ]; then
	echo $USERPASS | sudo -S dpkg -i libpmemkv*.deb
elif [ "${PACKAGE_MANAGER}" = "RPM" ]; then
	echo $USERPASS | sudo -S RPM -i libpmemkv*.rpm
fi

#
# 2) Build and install JNI
#
cd $WORKDIR
git clone https://github.com/pmem/pmemkv-jni.git
cd pmemkv-jni
git checkout $JNI_VERSION
make test
echo $USERPASS | sudo -S make install prefix=$PREFIX

#
# 3) JAVA dependencies - all of the dependencies needed to run
#                        pmemkv-java will be saved
#                        in the /opt/java directory
cd $WORKDIR
mkdir /opt/java/
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

git clone https://github.com/pmem/pmemkv-java.git
cd pmemkv-java
git checkout $JAVA_VERSION
mvn dependency:go-offline
mvn install
mv -v ~/.m2/repository /opt/java/

#
# Uninstall all unneeded stuff
#
if [ "${PACKAGE_MANAGER}" = "DEB" ]; then
	echo $USERPASS | sudo -S dpkg -r $(apt list --installed | grep -e libpmemkv | cut -d'/' -f1)
elif [ "${PACKAGE_MANAGER}" = "RPM" ]; then
	echo $USERPASS | sudo -S RPM -e $(rpm -qa | grep -e libpmemkv)
fi

cd $WORKDIR
rm -r pmemkv-jni pmemkv-java

# make the /opt/java directory world-readable
chmod -R a+r /opt/java
