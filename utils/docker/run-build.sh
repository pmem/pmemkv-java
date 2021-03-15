#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# run-build.sh <pmemkv_version> - is called inside a Docker container;
#		checks bindings' building and installation with given version of pmemkv
#

set -e

source `dirname $0`/prepare-for-build.sh
MVN_PARAMS="${PMEMKV_MVN_PARAMS}"

function run_example() {
	example_name=${1}
	# Find current pmemkv-binding package and path to example's jar
	jar_path=$(find ../pmemkv-binding/target/ | grep -E "pmemkv-([0-9.]+).jar")
	example_path=$(find . | grep -E "${example_name}-([0-9.]+).jar$")

	java -ea -Xms1G -cp ${jar_path}:${example_path} ${example_name}
}

# install pmemkv
pmemkv_version=$1
cd /opt/pmemkv-$pmemkv_version/
if [ "${PACKAGE_MANAGER}" = "deb" ]; then
	sudo_password dpkg -i libpmemkv*.deb
elif [ "${PACKAGE_MANAGER}" = "rpm" ]; then
	sudo_password rpm -i libpmemkv*.rpm
else
	echo "PACKAGE_MANAGER env variable not set or set improperly ('deb' or 'rpm' supported)."
	exit 1
fi

echo
echo "###########################################################"
echo "### Verifying building and installing of the java bindings"
echo "###########################################################"
cd $WORKDIR
mkdir -p ~/.m2/repository
cp -r /opt/java/repository ~/.m2/
mvn install -e ${MVN_PARAMS}

echo
echo "###########################################################"
echo "### Verifying building and execution of examples"
echo "###########################################################"
cd examples
run_example StringExample
run_example ByteBufferExample
run_example MixedTypesExample
# PicturesExample is a GUI application, so just test compilation.
run_example PicturesExample
