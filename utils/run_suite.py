#!/usr/bin/python2
import json
import os
import subprocess

#comment
# SUITE write_workload
# THREADS 1 2 4 8 16 32 48 64 96
# JOURNALING enabled/disabled
# RECORDS 1000
# OPERATIONS 100
# READ_PROPORTION 0.0
# UPDATE_PROPORTION 0.0
# INSERT_PROPORTION 1.0
# YCSB_NUMA 1
# DROP_BEFORE
# ENDSUITE

#GET PATHS FROM CONFIG FILE
PATH_TO_YCSB = ''

path_configuration = open("path_configuration.txt", "r")
for line in path_configuration:
    if line.startswith('YCSB_PATH='):
        arg = line.split("=")
        if len(arg) > 1:
            PATH_TO_YCSB = arg[1].replace('\n','')
        else:
            raise NameError('No path in YCSB_PATH!')
            
if not os.path.isdir(PATH_TO_YCSB):
    raise NameError('Wrong path to YCSB!')

class Test:
    def __init__(self):
        self.pmemkv_engine = "cmap"
        self.pmemkv_dbsize = 0
        self.pmemkv_dbpath = "/dev/shm/"
        self.workload_type = "workloada"
        self.testName = ""
        self.threads = []
#        self.journaling = ""
        self.records = 0
        self.operations = 0
        self.read_proportion = -1.0
        self.update_proportion = -1.0
        self.insert_proportion = -1.0
        self.ycsb_numa = -1
# Actually we don't need creation
#        self.drop_before = -1
#        self.create_after_drop = -1
        self.is_load = -1
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, 
                          sort_keys=True, indent=4)

def getArgs(str):
    arguments = []
    for i in range(1, len(str)):
        arguments.append(str[i])
    return arguments

KEYWORDS = set(["THREADS", "JOURNALING", "RECORDS", "OPERATIONS",
                "READ_PROPORTION", "LOAD", "UPDATE_PROPORTION",
                "INSERT_PROPORTION", "YCSB_NUMA", "SUITE", "ENDSUITE",
                "DROP_BEFORE", "CREATE_AFTER_DROP", "PMEMKV_ENGINE",
                "PMEMKV_DBSIZE", "PMEMKV_DBPATH", "WORKLOAD_TYPE"]) #Add keyword if you need to extend implementation

# open meta file
with open("test_suite.txt", "r") as configfile:
    configurations = []
    for line in configfile:
        splittedLine = line.split()
        if line == '\n' or line.startswith('#'):
            continue
        if len(set.intersection(KEYWORDS, splittedLine)) != 1:
            print(splittedLine)
            raise NameError('Too many keywords in single line!')

        #get args if exists
        args = getArgs(splittedLine)
        
        #if line starts from keyword we must read arguments
        if splittedLine[0] == "SUITE":
            configurations.append(Test())
            configurations[len(configurations)-1].testName = args[0]
        elif splittedLine[0] == "THREADS":
            configurations[len(configurations)-1].threads = args
        elif splittedLine[0] == "LOAD":
            configurations[len(configurations)-1].is_load = 1
        elif splittedLine[0] == "RECORDS":
            configurations[len(configurations)-1].records = args[0]
        elif splittedLine[0] == "OPERATIONS":
            configurations[len(configurations)-1].operations = args[0]
        elif splittedLine[0] == "READ_PROPORTION":
            configurations[len(configurations)-1].read_proportion = args[0]
        elif splittedLine[0] == "UPDATE_PROPORTION":
            configurations[len(configurations)-1].update_proportion = args[0]
        elif splittedLine[0] == "INSERT_PROPORTION":
            configurations[len(configurations)-1].insert_proportion = args[0]
        elif splittedLine[0] == "YCSB_NUMA":
            configurations[len(configurations)-1].ycsb_numa = args[0]
        elif splittedLine[0] == "PMEMKV_ENGINE":
            configurations[len(configurations)-1].pmemkv_engine = args[0]
        elif splittedLine[0] == "PMEMKV_DBSIZE":
            configurations[len(configurations)-1].pmemkv_dbsize = args[0]
        elif splittedLine[0] == "PMEMKV_DBPATH":
            configurations[len(configurations)-1].pmemkv_dbpath = args[0]
        elif splittedLine[0] == "WORKLOAD_TYPE":
            configurations[len(configurations)-1].workload_type = args[0]
        elif splittedLine[0] == "ENDSUITE":
            continue
        else:
            raise NameError('Unrecognized keyword')
configfile.close()

print('Script read those tests:')
i = 1
for conf in configurations:
    print('{:>20} {:<12}'.format('Test#: ', str(i)))
    print('{:>20} {:<12}'.format("Name: ", conf.testName))
    print('{:>20} {:<12}'.format("Threads: " ,str(conf.threads)))
    print('{:>20} {:<12}'.format("Records: ", conf.records))
    print('{:>20} {:<12}'.format("Operation: ", conf.operations))
    print('{:>20} {:<12}'.format("Read proportion: ", str(conf.read_proportion)))
    print('{:>20} {:<12}'.format("Update proportion: ", str(conf.update_proportion)))
    print('{:>20} {:<12}'.format("Insert proportion: ", str(conf.insert_proportion)))
    print('{:>20} {:<12}'.format("Is load: ", str(conf.is_load)))
    print('{:>20} {:<12}'.format("NUMA for YCSB: ", conf.ycsb_numa))
    print('{:>20} {:<12}'.format("Workload type: ", conf.workload_type))
    print('{:>20} {:<12}'.format("Pmemkv engine: ", conf.pmemkv_engine))
    print('{:>20} {:<12}'.format("Pmemkv size: ", conf.pmemkv_dbsize))
    print('{:>20} {:<12}'.format("Pmemkv path: ", conf.pmemkv_dbpath))
    print("")
    i = i + 1

# PUT CONFIGURATION TO FILE IN PROPER PATH
results_directory = "results/"
if not os.path.exists(results_directory):
    os.makedirs(results_directory)
i = 1
with open(results_directory + '/configurations.json', 'w') as jsonconfig:
    for conf in configurations:
        jsonconfig.write(conf.toJSON() + '\n')
        if not os.path.exists(results_directory + conf.testName + '/'):
                os.makedirs(results_directory + conf.testName + '/')
        with open(results_directory + conf.testName + '/test_description.txt', 'a') as test_description:
            test_description.write('{:>20} {:<12}'.format('Test#: ', str(i)) + '\n') #   'Test #' + str(i)
            test_description.write('{:>20} {:<12}'.format("Name: ", conf.testName) + '\n')
            test_description.write('{:>20} {:<12}'.format("Threads: " ,str(conf.threads)) + '\n')
            test_description.write('{:>20} {:<12}'.format("Records: ", conf.records) + '\n')
            test_description.write('{:>20} {:<12}'.format("Operation: ", conf.operations) + '\n')
            test_description.write('{:>20} {:<12}'.format("Read proportion: ", str(conf.read_proportion)) + '\n')
            test_description.write('{:>20} {:<12}'.format("Update proportion: ", str(conf.update_proportion)) + '\n')
            test_description.write('{:>20} {:<12}'.format("Insert proportion: ", str(conf.insert_proportion)) + '\n')
            test_description.write('{:>20} {:<12}'.format("NUMA for YCSB: ", conf.ycsb_numa) + '\n')
            test_description.write('{:>20} {:<12}'.format("Workload type: ", conf.workload_type) + '\n')
            test_description.write('{:>20} {:<12}'.format("Pmemkv engine: ", conf.pmemkv_engine) + '\n')
            test_description.write('{:>20} {:<12}'.format("Pmemkv size: ", conf.pmemkv_dbsize) + '\n')
            test_description.write('{:>20} {:<12}'.format("Pmemkv path: ", conf.pmemkv_dbpath) + '\n')
            test_description.write('\n')
        i = i + 1

# run specified configurations
generated_commands = []
for test in configurations:
    command_prefix = ''
    command_suffix = ''
    
    command_prefix = './run_workload.sh ' + test.testName
    
    if not test.is_load == 1:
        command_prefix += ' run '
    else:
        command_prefix += ' load '


    # Put path to YCSB main directory
    command_suffix += PATH_TO_YCSB + ' '
    # Put operation numbers
    command_suffix += test.records + ' ' + test.operations + ' '
    # Put workload ratios
    command_suffix += test.read_proportion + ' ' + test.update_proportion + ' ' + test.insert_proportion + ' '
    # Put NUMA node
    if test.ycsb_numa == -1:
        print('NUMA node is not set for test: ' + test.testName + '.')
    command_suffix += test.ycsb_numa + ' '
    # Put workload type
    command_suffix += test.workload_type + ' '
    # Put engine specific fields
    command_suffix += test.pmemkv_engine + ' ' + test.pmemkv_dbsize + ' ' + test.pmemkv_dbpath + ' '

    for thread_no in test.threads:
        # DROP BEFORE LOAD PHASE
        #if test.drop_before == 1 or test.create_after_drop == 1 or test.is_load == 1:
        ##    generated_commands.append(PATH_TO_MONGO + 'mongo ' + PATH_TO_MONGO + 'drop_table.js')
        #if test.create_after_drop == 1:
        #    generated_commands.append(PATH_TO_MONGO + 'mongo ' + PATH_TO_MONGO + 'create_table.js')

        # DROP&CREATE BEFORE NEXT INSERTS
        generated_commands.append(command_prefix + thread_no + ' ' + command_suffix)

# Generate script
with open('testplan.sh','w') as testplan:
    testplan.write('#!/bin/bash\n')
    for x in generated_commands:
        testplan.write(x + '\n')
print(generated_commands)
