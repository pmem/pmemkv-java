#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2021, Intel Corporation

#
# run-build.sh <pmemkv_version> - is called inside a Docker container;
#		checks bindings' building and installation with given version of pmemkv
#

set -e

source $(dirname ${0})/prepare-for-build.sh
MVN_PARAMS="${PMEMKV_MVN_PARAMS}"

# install pmemkv
pmemkv_version=${1}
install_pmemkv ${pmemkv_version}

echo
echo "###########################################################"
echo "### Verifying building and installing of the java bindings"
echo "###########################################################"
use_preinstalled_java_deps
cd ${WORKDIR}
mvn install -e ${MVN_PARAMS}

echo
echo "###########################################################"
echo "### Verifying execution of examples"
echo "###########################################################"
cd examples
run_example StringExample
run_example ByteBufferExample
run_example MixedTypesExample
# PicturesExample is a GUI application, so just test compilation.
run_example PicturesExample
