/*
 * Copyright (c) 2022 nosqlbench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nosqlbench.adapter.keyspaces;

/**
 * Op templates which are supported by the NoSQLBench Amazon Keyspaces driver are
 * enumerated below. These command names should mirror those in the official
 * Keyspaces API exactly. See the official API for more details.
 * @see <a href="https://docs.aws.amazon.com/keyspacesClient/latest/APIReference/Welcome.html">Amazon Keyspaces API Reference</a>
 */
public enum KeyspacesCmdType {
    CreateKeyspace
//	,CreateTable
//	,DeleteKeyspace,
//    DeleteTable,
//    GetKeyspace,
//    GetTable,
//    UpdateTable
}
