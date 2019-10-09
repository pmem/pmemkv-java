prefix=/usr/local

all: test

clean:
	rm -rf ./build googletest-*.zip

configure:
	mkdir -p ./build
	cd ./build && cmake .. -DCMAKE_BUILD_TYPE=Release

sharedlib: configure
	cd ./build && make pmemkv-jni

install:
	cp ./build/libpmemkv-jni.so $(prefix)/lib

uninstall:
	rm -rf $(prefix)/lib/libpmemkv-jni.so

test: sharedlib
	cd ./build && make pmemkv-jni_test
	PMEM_IS_PMEM_FORCE=1 ./build/pmemkv-jni_test
