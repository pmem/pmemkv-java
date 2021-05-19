#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# run-maven-example.sh - is called inside a Docker container;
#		checks building of examples using pmemkv from maven repository
#

set -e

source $(dirname ${0})/prepare-for-build.sh
MVN_PARAMS="${PMEMKV_MVN_PARAMS}"

echo
echo "#############################################################"
echo "### Apply patch (to use pmemkv from maven) and build examples"
echo "#############################################################"
cd ${WORKDIR}
git apply utils/docker/0001-examples-use-pmemkv-from-maven-repo.patch

cd examples
mvn package -e ${PMEMKV_MVN_PARAMS}

echo
echo "#############################################################"
echo "### Verify execution of examples"
echo "#############################################################"
run_standalone_example StringExample
run_standalone_example ByteBufferExample
run_standalone_example MixedTypesExample
# PicturesExample is a GUI application, so just test compilation.
run_standalone_example PicturesExample
