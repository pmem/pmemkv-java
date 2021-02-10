#!/usr/bin/env bash
# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2021, Intel Corporation

#
# setup-maven-settings.sh - setup some extra settings for maven
#

set -e

# Split proxies into ip & port; remove possible "/"
http_proxy_ip=$(echo ${http_proxy} | cut -d: -f2 | sed 's/\///g')
http_proxy_port=$(echo ${http_proxy} | cut -d: -f3 | sed 's/\///g')
https_proxy_ip=$(echo ${https_proxy} | cut -d: -f2 | sed 's/\///g')
https_proxy_port=$(echo ${https_proxy} | cut -d: -f3 | sed 's/\///g')

mvn_alias='mvn'
[ -n "${http_proxy_ip}" ] && mvn_alias="${mvn_alias} -Dhttp.proxyHost=${http_proxy_ip}"
[ -n "${http_proxy_port}" ] && mvn_alias="${mvn_alias} -Dhttp.proxyPort=${http_proxy_port}"
[ -n "${https_proxy_ip}" ] && mvn_alias="${mvn_alias} -Dhttps.proxyHost=${https_proxy_ip}"
[ -n "${https_proxy_port}" ] && mvn_alias="${mvn_alias} -Dhttps.proxyPort=${https_proxy_port}"

# add mvn alias for all users
echo "alias mvn='${mvn_alias}'" >> /etc/skel/.bashrc
