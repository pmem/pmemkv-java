#!/bin/bash
# Run workload from command line
# ./run_workload.sh suite_name(string) workload_type(string) no_threads(uint) 
#                   journal(bool) record_count(uint) operation_count(uint)
#                   readproportion(uint) updateproportion(uint) insertproportion(uint)
#                   numa_node(uint) ycsb_path
#
# workload_type can be: run or load according to YCSB documentation
#
# e.g. ./run_workload.sh write run 64 true 1000000 10000000 0.0 0.0 1.0 
#
# 1 - 
#
#
#
#
#
#
#
#
#
#YCSB_PATH=/home/kfilipek/repository/mongo_repositories/YCSB/
YCSB_PATH=${11}
echo $YCSB_PATH
OLD_PATH=$(pwd)

echo $0 $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11}
echo "Passed $# argumets to script"
if [ $# -ne 11 ]; 
then
	echo "Illegal number of parameters, should be 11. Check script documentation."
	exit 0
fi

if [ $4 = "true" ];
then
	JOURNALING=journaled
else
	JOURNALING=acknowledged
fi

mkdir -p "results/$1/"

if [ $2 = "load" ];
then
	# LOAD PHASE
	echo "load chosen"
	if [ ${10} -lt 0 ];
	then
    	cd $YCSB_PATH
	    ./bin/ycsb load mongodb -s -threads $3 -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p readproportion=$7 -p updateproportion=$8 -p insertproportion=$9 -P ./workloads/workloada -p mongodb.url=mongodb://localhost:27017/ycsb -p mongodb.writeConcern=$JOURNALING > $OLD_PATH/results/$1/load_$3.log
	    cd $OLD_PATH
	else
	    cd $YCSB_PATH
    	numactl -N ${10} ./bin/ycsb load mongodb -s -threads $3 -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p readproportion=$7 -p updateproportion=$8 -p insertproportion=$9 -P ./workloads/workloada -p mongodb.url=mongodb://localhost:27017/ycsb -p mongodb.writeConcern=$JOURNALING > $OLD_PATH/results/$1/load_$3.log
        cd $OLD_PATH
	fi
else
	# RUN PHASE
	echo "run chosen"
	if [ ${10} -lt 0 ];
	then
	    cd $YCSB_PATH
    	./bin/ycsb run mongodb -s -threads $3 -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p readproportion=$7 -p updateproportion=$8 -p insertproportion=$9 -P ./workloads/workloada -p mongodb.url=mongodb://localhost:27017/ycsb -p mongodb.writeConcern=$JOURNALING > $OLD_PATH/results/$1/run_$3.log
    	cd $OLD_PATH
    else
        cd $YCSB_PATH
        numactl -N ${10} ./bin/ycsb run mongodb -s -threads $3 -p hdrhistogram.percentiles=95,99,99.9,99.99 -p recordcount=$5 -p operationcount=$6 -p readproportion=$7 -p updateproportion=$8 -p insertproportion=$9 -P ./workloads/workloada -p mongodb.url=mongodb://localhost:27017/ycsb -p mongodb.writeConcern=$JOURNALING > $OLD_PATH/results/$1/run_$3.log
        cd $OLD_PATH
    fi
fi
