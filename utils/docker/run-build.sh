#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# run-build.sh <pmemkv_version> - is called inside a Docker container;
#        checks bindings' building and installation with given version of pmemkv
#

set -e

source `dirname $0`/prepare-for-build.sh

# install pmemkv
pmemkv_version=$1
cd /opt/pmemkv-$pmemkv_version/
if [ "${PACKAGE_MANAGER}" = "deb" ]; then
	echo $USERPASS | sudo -S dpkg -i libpmemkv*.deb
elif [ "${PACKAGE_MANAGER}" = "rpm" ]; then
	echo $USERPASS | sudo -S rpm -i libpmemkv*.rpm
else
	echo "PACKAGE_MANAGER env variable not set or set improperly ('deb' or 'rpm' supported)."
	exit 1
fi

echo "#####################################################"
echo "### Verifying building and tests execution of the jni"
echo "#####################################################"
mkdir $WORKDIR/build
cd $WORKDIR/build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc) pmemkv-jni
make -j$(nproc) pmemkv-jni_test
PMEM_IS_PMEM_FORCE=1 ./pmemkv-jni_test

# check if the library exists
ls -al libpmemkv-jni.so

echo
echo "###########################################################"
echo "### Verifying building and installing of the java bindings "
echo "###########################################################"
cd $WORKDIR
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${WORKDIR}/build
mkdir -p ~/.m2/repository
cp -r /opt/java/repository ~/.m2/
mvn install
