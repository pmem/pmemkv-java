#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2016-2021, Intel Corporation

#
# build-image.sh <OS-OS_VER> - prepares a Docker image with <OS>-based
#                           environment for testing pmemkv-java, according
#                           to the Dockerfile.<OS-OS_VER> file located
#                           in the same directory.
#
# The script can be run locally.
#

set -e

OS__OS_VER=$1
TAG="1.1-${OS__OS_VER}"

function usage {
	echo "Usage:"
	echo "    build-image.sh <OS-OS_VER>"
	echo "where <OS-OS_VER>, for example, can be 'fedora-32', provided " \
		"a Dockerfile named 'Dockerfile.fedora-32' exists in the " \
		"current directory."
}

# Check if the argument is not empty
if [[ -z "$1" ]]; then
	usage
	exit 1
fi

if [[ -z "${CONTAINER_REG}" ]]; then
	echo "ERROR: CONTAINER_REG environment variable is not set " \
		"(e.g. \"<registry_addr>/<org_name>/<package_name>\")."
	exit 1
fi

# Check if the file Dockerfile.OS-OS_VER exists
if [[ ! -f "Dockerfile.$OS__OS_VER" ]]; then
	echo "Error: Dockerfile.$OS__OS_VER does not exist."
	echo
	usage
	exit 1
fi

echo "Build a Docker image tagged with: ${CONTAINER_REG}:${TAG}"
docker build -t ${CONTAINER_REG}:${TAG} \
	--build-arg http_proxy=$http_proxy \
	--build-arg https_proxy=$https_proxy \
	-f Dockerfile.${OS__OS_VER} .
