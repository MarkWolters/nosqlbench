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

package io.nosqlbench.adapter.keyspaces.optypes;

import software.amazon.awssdk.services.keyspaces.KeyspacesClient;

public class RawKeyspacesOp extends KeyspacesOp {
    public RawKeyspacesOp(KeyspacesClient keyspacesClient, String body) {
        super(keyspacesClient);
    }

    @Override
    public KeyspacesClient apply(long value) {
        throw new RuntimeException("raw ops are not supported in this API yet");
    }
}
