// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2019, Intel Corporation */

#include "io_pmem_pmemkv_Database.h"
#include "gtest/gtest.h"

int main(int argc, char* argv[]) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}

class KVEmptyTest : public testing::Test {
  public:
    KVEmptyTest() {
    }
};

TEST_F(KVEmptyTest, DoNothingTest) {
}
