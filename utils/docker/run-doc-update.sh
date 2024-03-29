#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2018-2022, Intel Corporation

#
# run-doc-update.sh - is called inside a Docker container,
#		to build docs for 'valid branches' and to create a pull request
#		with and update of javadocs, html files (on 'docs' branch).
#

set -e

source $(dirname ${0})/prepare-for-build.sh

if [[ -z "${DOC_UPDATE_GITHUB_TOKEN}" || -z "${DOC_UPDATE_BOT_NAME}" || -z "${DOC_REPO_OWNER}" ]]; then
	echo "To build documentation and upload it as a Github pull request, variables " \
		"'DOC_UPDATE_BOT_NAME', 'DOC_REPO_OWNER' and 'DOC_UPDATE_GITHUB_TOKEN' have to " \
		"be provided. For more details please read CONTRIBUTING.md"
	exit 0
fi

# Set up required variables
BOT_NAME=${DOC_UPDATE_BOT_NAME}
DOC_REPO_OWNER=${DOC_REPO_OWNER}
REPO_NAME=${REPO:-"pmemkv-java"}
export GITHUB_TOKEN=${DOC_UPDATE_GITHUB_TOKEN} # export for hub command
REPO_DIR=$(mktemp -d -t pmemkvjava-XXX)
ARTIFACTS_DIR=$(mktemp -d -t ARTIFACTS-XXX)
MVN_PARAMS="${PMEMKV_MVN_PARAMS}"

# Only 'master' or 'stable-*' branches are valid; determine docs location dir on 'docs' branch
TARGET_BRANCH=${CI_BRANCH}
if [[ "${TARGET_BRANCH}" == "master" ]]; then
	TARGET_DOCS_DIR="master"
elif [[ ${TARGET_BRANCH} == stable-* ]]; then
	TARGET_DOCS_DIR=v$(echo ${TARGET_BRANCH} | cut -d"-" -f2 -s)
else
	echo "Skipping docs build, this script should be run only on master or stable-* branches."
	echo "TARGET_BRANCH is set to: \'${TARGET_BRANCH}\'."
	exit 0
fi
if [ -z "${TARGET_DOCS_DIR}" ]; then
	echo "ERROR: Target docs location for branch: ${TARGET_BRANCH} is not set."
	exit 1
fi

ORIGIN="https://${GITHUB_TOKEN}@github.com/${BOT_NAME}/${REPO_NAME}"
UPSTREAM="https://github.com/${DOC_REPO_OWNER}/${REPO_NAME}"

install_pmemkv master

pushd ${REPO_DIR}
echo "Clone repo:"
git clone ${ORIGIN} ${REPO_DIR}
cd ${REPO_DIR}
git remote add upstream ${UPSTREAM}

git config --local user.name ${BOT_NAME}
git config --local user.email "${BOT_NAME}@intel.com"
hub config --global hub.protocol https

git remote update
git checkout -B ${TARGET_BRANCH} upstream/${TARGET_BRANCH}

echo "Build docs:"
use_preinstalled_java_deps
mvn install -Dmaven.test.skip=true -e ${MVN_PARAMS}
mvn javadoc:javadoc -e ${MVN_PARAMS}
cp -r ${REPO_DIR}/pmemkv-binding/target/site/apidocs ${ARTIFACTS_DIR}/

# Checkout 'docs' branch and copy docs there
GH_PAGES_NAME="${TARGET_DOCS_DIR}-docs-update"
git checkout -B ${GH_PAGES_NAME} upstream/docs
git clean -dfx

# Clean old content, since some files might have been deleted
rm -rf ./${TARGET_DOCS_DIR}
mkdir -p ./${TARGET_DOCS_DIR}/html/

cp -rf ${ARTIFACTS_DIR}/apidocs/* ./${TARGET_DOCS_DIR}/html/

echo "Add and push changes:"
# git commit command may fail if there is nothing to commit.
# In that case we want to force push anyway (there might be open pull request with
# changes which were reverted).
git add -A
git commit -m "doc: automatic docs update" && true
git push -f ${ORIGIN} ${GH_PAGES_NAME}

echo "Make or update pull request:"
# When there is already an open PR or there are no changes an error is thrown, which we ignore.
hub pull-request -f -b ${DOC_REPO_OWNER}:docs -h ${BOT_NAME}:${GH_PAGES_NAME} \
	-m "doc: automatic docs update for ${TARGET_BRANCH}" && true

popd
