import os
from os.path import join, getsize

for root, dirs, filenames in os.walk('results'):
    if len(dirs) == 0:
        parsed_results = []
        for filename in filenames:
            if filename.split('_')[0] == 'run':
                with open(root + '/' + filename) as file_object:
                    file_object.readline()
                    trimmed_lines = []
                    for line in file_object.readlines():
                        record = tuple(line.replace(',','').split(' '))
                        if record[0] != '[CLEANUP]' or record[0] != '[READ-FAILED]':
                            if record[0] == '[READ]' or record[0] == '[INSERT]' or record[0] == '[UPDATE]' or record[0] == '[OVERALL]': #in case of READ
                                try:
                                    int(record[1])
                                except ValueError: #if cannot cast it's fine
                                    trimmed_lines.append(record)
                    parsed_results.append([int(filename.split('_')[1].split('.')[0]), trimmed_lines])

        parsed_results = sorted(parsed_results, key=lambda x: x[0], reverse=False)
        csv = []
        print root
        threads = 'Threads;#;'
        if len(parsed_results) <= 0:
            continue
        print '------CSV------'
        for i in range(0, len(parsed_results[0][1])):
            csv.append(parsed_results[0][1][i][0] + ';' + parsed_results[0][1][i][1] + ';')
        for test_result in parsed_results:
            threads += str(test_result[0]) + ';'
            for i, line in enumerate(test_result[1]):
                csv[i] += line[2].replace('\n','').replace('.',',') + ';'
        csv.insert(0, threads)
        with open(root + '/results.csv','w') as csv_file:
            for x in csv:
                csv_file.write(x + '\n')
                print x
            csv_file.close()
