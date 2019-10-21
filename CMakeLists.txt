# Copyright 2017-2019, Intel Corporation
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in
#       the documentation and/or other materials provided with the
#       distribution.
#
#     * Neither the name of the copyright holder nor the names of its
#       contributors may be used to endorse or promote products derived
#       from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

cmake_minimum_required(VERSION 3.5)
project(pmemkv-jni)

set(CMAKE_CXX_STANDARD 11)
set(SOURCE_FILES src/io_pmem_pmemkv_Database.cpp src/io_pmem_pmemkv_Database.h)
set(3RDPARTY ${PROJECT_SOURCE_DIR}/3rdparty)
set(GTEST_VERSION 1.7.0)

find_package(PkgConfig QUIET)
include(ExternalProject)
include(FindThreads)

include_directories("${source_dir}/include")

find_package(JNI)
include_directories(${JNI_INCLUDE_DIRS})
link_directories(${JNI_LIBRARIES})

add_library(pmemkv-jni SHARED ${SOURCE_FILES})
target_link_libraries(pmemkv-jni pmemkv pmemkv_json_config)

# CMake option 'CMAKE_PREFIX_PATH' will be prioritized
# over system paths in find_library and find_path calls
find_library(GTEST NAMES gtest)
if(GTEST)
	message(STATUS "Gtest found in ${GTEST}")

	find_path(GTEST_INCLUDEDIR "gtest/gtest.h")
	if (GTEST_INCLUDEDIR)
		message(STATUS "Gtest headers found in ${GTEST_INCLUDEDIR}")
		include_directories(${GTEST_INCLUDEDIR})
	endif()

	add_library(libgtest IMPORTED STATIC GLOBAL)
	set_target_properties(libgtest PROPERTIES "IMPORTED_LOCATION" "${GTEST}"
		"IMPORTED_LINK_INTERFACE_LIBRARIES" "${CMAKE_THREAD_LIBS_INIT}")

	add_executable(pmemkv-jni_test src/pmemkv-jni_test.cc)
	target_link_libraries(pmemkv-jni_test pmemkv libgtest pthread)
else()
	message(FATAL_ERROR "Gtest is not installed or couldn't be found. Try using cmake option "
			"-DCMAKE_PREFIX_PATH=<dir with lib and include dirs for gtest>. "
			"Use e.g. like 'cmake .. -DCMAKE_PREFIX_PATH=/install/path/gtest/'")
endif()
