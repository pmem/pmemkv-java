#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2019-2020, Intel Corporation

#
# prepare-for-build.sh - prepare the Docker image for the build
#

set -e

PREFIX=/usr

function sudo_password() {
	echo $USERPASS | sudo -Sk $*
}

function install_pmemkv() {
	pmemkv_version=${1}
	echo "Install pmemkv (${pmemkv_version}):"

	cd /opt/pmemkv-$pmemkv_version/
	if [ "${PACKAGE_MANAGER}" = "deb" ]; then
		sudo_password dpkg -i libpmemkv*.deb
	elif [ "${PACKAGE_MANAGER}" = "rpm" ]; then
		sudo_password rpm -i libpmemkv*.rpm
	else
		echo "PACKAGE_MANAGER env variable not set or set improperly ('deb' or 'rpm' supported)."
		exit 1
	fi
	cd -
}

function use_preinstalled_java_deps() {
	mkdir -p ~/.m2/repository
	cp -r /opt/java/repository ~/.m2/
}

function run_example() {
	example_name=${1}
	# Find current pmemkv-binding package and path to example's jar
	jar_path=$(find ../pmemkv-binding/target/ | grep -E "pmemkv-([0-9.]+).jar")
	example_path=$(find . | grep -E "${StringExample}-([0-9.]+).jar$")

	java -ea -Xms1G -cp ${jar_path}:${example_path} ${example_name}
}

function run_standalone_example() {
	example_name=${1}
	# Find pmemkv package (in the system) and path to example's jar
	jar_path=$(find ${HOME}/.m2/repository/io/pmem | grep -E "pmemkv-([root\-][0-9.]+).jar$")
	example_path=$(find . | grep -E "${example_name}-([0-9.]+).jar$")

	java -ea -Xms1G -cp ${jar_path}:${example_path} ${example_name}
}

# this should be run only on CIs
if [ "$CI_RUN" == "YES" ]; then
	sudo_password chown -R $(id -u).$(id -g) $WORKDIR
fi || true
