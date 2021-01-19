#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2018-2021, Intel Corporation

#
# run-doc-update.sh - is called inside a Docker container,
#		to build docs for 'valid branches' and to create a pull request
#		with and update of javadocs, html files (on gh-pages).
#

set -e

source $(dirname ${0})/valid-branches.sh

# Set up required variables
BOT_NAME="pmem-bot"
USER_NAME="pmem"
REPO_NAME="pmemkv-java"
export GITHUB_TOKEN=${GITHUB_TOKEN} # export for hub command
REPO_DIR=$(mktemp -d -t pmemkvjava-XXX)
ARTIFACTS_DIR=$(mktemp -d -t ARTIFACTS-XXX)

ORIGIN="https://${GITHUB_TOKEN}@github.com/${BOT_NAME}/${REPO_NAME}"
UPSTREAM="https://github.com/${USER_NAME}/${REPO_NAME}"
# master or stable-* branch
TARGET_BRANCH=${CI_BRANCH}
TARGET_DOCS_DIR=${TARGET_BRANCHES[$TARGET_BRANCH]}

if [ -z $TARGET_DOCS_DIR ]; then
	echo "Target location for branch ${TARGET_BRANCH} is not defined."
	exit 1
fi

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
mvn javadoc:javadoc -e
cp -r ${REPO_DIR}/pmemkv-binding/target/site/apidocs ${ARTIFACTS_DIR}/

# Checkout gh-pages and copy docs
GH_PAGES_NAME="${TARGET_DOCS_DIR}-gh-pages-update"
git checkout -B ${GH_PAGES_NAME} upstream/gh-pages
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
git commit -m "doc: automatic gh-pages docs update" && true
git push -f ${ORIGIN} ${GH_PAGES_NAME}

echo "Make or update pull request:"
# When there is already an open PR or there are no changes an error is thrown, which we ignore.
hub pull-request -f -b ${USER_NAME}:gh-pages -h ${BOT_NAME}:${GH_PAGES_NAME} \
	-m "doc: automatic gh-pages update for ${TARGET_BRANCH}" && true

popd
