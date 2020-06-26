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

echo
echo "###########################################################"
echo "### Verifying building and installing of the java bindings "
echo "###########################################################"
cd $WORKDIR
mkdir -p ~/.m2/repository
cp -r /opt/java/repository ~/.m2/
mvn install -e
