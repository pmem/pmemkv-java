prefix=/usr/local

all: clean example test

clean:
	rm -rf ./bin googletest-*.zip

configure:
	mkdir -p ./bin
	cd ./bin && cmake .. -DCMAKE_BUILD_TYPE=Release

sharedlib:
	cd ./bin && make pmemkv-jni

install: sharedlib
	cp ./bin/libpmemkv-jni.so $(prefix)/lib

uninstall:
	rm -rf $(prefix)/lib/libpmemkv-jni.so

test: configure
	cd ./bin && make pmemkv-jni_test
	PMEM_IS_PMEM_FORCE=1 ./bin/pmemkv-jni_test
