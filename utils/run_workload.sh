#!/bin/bash

# SPDX-License-Identifier: BSD-3-Clause
# Copyright 2017-2022, Intel Corporation

# Run workload from command line
#
# e.g. ./run_workload.sh run_cmap run 12 PATH_TO_YCSB 1000000 1000000
#             {0}          {1}    {2} {3}   {4}         {5}     {6}
#                        -1.0 -1.0 -1.0   1  workloadb csmap 80000000 DBPATH
#                         {7}  {8}  {9} {10}    {11}   {12}    {13}    {14}
# 1 - suite name
# 2 - ycsb phase: load/run
# 3 - thread count
# 4 - path to YCSB
# 5 - record count
# 6 - operation count
# 7 - read proportion
# 8 - insert proportion
# 9 - update proportion
# 10 - NUMA node for YCSB
# 11 - workload scenario (workload[a-f])
####### Engine related args
# 12 - pmemkv: engine name
# 13 - pmemkv: pool size
# 14 - pmemkv: path to pool

YCSB_PATH=/home/kfilipek/Development/YCSB/ # TODO(kfilipek): remove hardcoding
echo $YCSB_PATH
OLD_PATH=$(pwd)

echo $@
echo "Passed $# argumets to script"

if [ "$#" -ne "14" ];
then
	echo "Illegal number of parameters, should be 11. Check script documentation."
	exit 0
fi

mkdir -p "results/$1/" # Create results directory: results/{test_suite_name}/
# Prepare future arguments for YCSB
NUMA_ARG=""
READ_RATIO=""
INSERT_RATIO=""
UPDATE_RATIO=""
if [ "$7" != "-1.0" ];
then
	READ_RATIO=" -p readproportion=$7 "
fi
if [ "$8" != "-1.0" ];
then
	INSERT_RATIO=" -p insertproportion=$8 "
fi
if [ "$9" != "-1.0" ];
then
	UPDATE_RATIO=" -p updateproportion=$9 "
fi
if [ "${10}" != "-1" ];
then
	NUMA_ARG=" numactl -N ${10} "
fi
# echo "READ_RATIO param: $READ_RATIO"
# echo "INSERT_RATIO param: $INSERT_RATIO"
# echo "UPDATE_RATIO param: $UPDATE_RATIO"
# echo "NUMA NODE param: $NUMA_ARG"
#exit

# TODOD(kfilipek): Implement splitting threads into processes
cd $YCSB_PATH
if [ "${2}" == "load" ];
then
	# Remove old DB before new load phase
	rm -rf ${14}
fi
echo "PMEM_IS_PMEM_FORCE=1 $NUMA_ARG bin/ycsb.sh $2 pmemkv -P workloads/${11} -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p pmemkv.engine=${12} -p pmemkv.dbsize=${13} -p pmemkv.dbpath=${14} > $OLD_PATH/results/$1/${2}_${3}.log" >> $OLD_PATH/results/$1/cmds_executed.log
PMEM_IS_PMEM_FORCE=1 $NUMA_ARG bin/ycsb.sh $2 pmemkv -P workloads/${11} -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p pmemkv.engine=${12} -p pmemkv.dbsize=${13} -p pmemkv.dbpath=${14} > $OLD_PATH/results/$1/${2}_${3}.log
cd $OLD_PATH
