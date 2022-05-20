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

package io.nosqlbench.adapter.keyspaces.opdispensers;

import io.nosqlbench.adapter.keyspaces.optypes.KeyspacesOp;
import io.nosqlbench.adapter.keyspaces.optypes.RawKeyspacesOp;
import io.nosqlbench.engine.api.activityimpl.BaseOpDispenser;
import io.nosqlbench.engine.api.templating.ParsedOp;
import software.amazon.awssdk.services.keyspaces.KeyspacesClient;

import java.util.function.LongFunction;

public class RawKeyspacesOpDispenser extends BaseOpDispenser<KeyspacesClient> {

    private final LongFunction<? extends String> jsonFunction;
    private final KeyspacesClient keyspacesClient;

    public RawKeyspacesOpDispenser(KeyspacesClient keyspacesClient, ParsedOp pOp) {
        super(pOp);
        this.keyspacesClient = keyspacesClient;

        String bodytype = pOp.getValueType("body").getSimpleName();
        switch (bodytype) {
            case "String":
                jsonFunction = pOp.getAsRequiredFunction("body");
                break;
            default:
                throw new RuntimeException("Unable to create body mapping function from type '" + bodytype + "'");
        }
    }

    @Override
    public KeyspacesOp apply(long value) {
        String body = jsonFunction.apply(value);
        return new RawKeyspacesOp(keyspacesClient, body);
    }
}
