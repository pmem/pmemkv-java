#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2021, Intel Corporation

#
# setup-maven-settings.sh - setup some extra settings for maven
#			It's executed in 'install-dependencies.sh', but it can also be
#			run locally (or in a custom docker container) to setup
#			these extra params for run-*.sh scripts.
#

# Split proxies into host & port; remove possible leftover "/"
if [[ -n "${http_proxy}" ]]; then
	http_proxy_ip=$(echo ${http_proxy} | cut -d: -f2 | sed 's/\///g')
	http_proxy_port=$(echo ${http_proxy} | cut -d: -f3 | sed 's/\///g')
fi
if [[ -n "${https_proxy}" ]]; then
	https_proxy_ip=$(echo ${https_proxy} | cut -d: -f2 | sed 's/\///g')
	https_proxy_port=$(echo ${https_proxy} | cut -d: -f3 | sed 's/\///g')
fi

mvn_params=''
[ -n "${http_proxy_ip}" ] && mvn_params="${mvn_params} -Dhttp.proxyHost=${http_proxy_ip}"
[ -n "${http_proxy_port}" ] && mvn_params="${mvn_params} -Dhttp.proxyPort=${http_proxy_port}"
[ -n "${https_proxy_ip}" ] && mvn_params="${mvn_params} -Dhttps.proxyHost=${https_proxy_ip}"
[ -n "${https_proxy_port}" ] && mvn_params="${mvn_params} -Dhttps.proxyPort=${https_proxy_port}"

# Export for current user/current shell
export PMEMKV_MVN_PARAMS="${mvn_params}"
