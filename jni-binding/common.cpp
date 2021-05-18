
// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2017-2021, Intel Corporation */

#include <common.h>

std::unordered_map<pmem::kv::status, const char*> PmemkvJavaException::PmemkvStatusDispatcher = {
       { pmem::kv::status::UNKNOWN_ERROR, "io/pmem/pmemkv/DatabaseException" },
       { pmem::kv::status::NOT_FOUND, "io/pmem/pmemkv/NotFoundException"},
       { pmem::kv::status::NOT_SUPPORTED, "io/pmem/pmemkv/NotSupportedException"},
       { pmem::kv::status::INVALID_ARGUMENT, "io/pmem/pmemkv/InvalidArgumentException"},
       { pmem::kv::status::CONFIG_PARSING_ERROR, "io/pmem/pmemkv/BuilderException"},
       { pmem::kv::status::CONFIG_TYPE_ERROR, "io/pmem/pmemkv/BuilderException"},
       { pmem::kv::status::STOPPED_BY_CB, "io/pmem/pmemkv/StoppedByCallbackException"},
       { pmem::kv::status::OUT_OF_MEMORY, "io/pmem/pmemkv/OutOfMemoryException"},
       { pmem::kv::status::WRONG_ENGINE_NAME, "io/pmem/pmemkv/WrongEngineNameException"},
       { pmem::kv::status::TRANSACTION_SCOPE_ERROR, "io/pmem/pmemkv/TransactionScopeException"}
};
