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

import io.nosqlbench.adapter.keyspaces.opdispensers.*;
import io.nosqlbench.adapter.keyspaces.optypes.KeyspacesOp;
import io.nosqlbench.engine.api.activityimpl.OpDispenser;
import io.nosqlbench.engine.api.activityimpl.OpMapper;
import io.nosqlbench.engine.api.activityimpl.uniform.DriverSpaceCache;
import io.nosqlbench.engine.api.templating.ParsedOp;
import io.nosqlbench.engine.api.templating.TypeAndTarget;
import io.nosqlbench.nb.api.config.standard.NBConfiguration;
import software.amazon.awssdk.services.keyspaces.KeyspacesClient;

public class KeyspacesOpMapper implements OpMapper<KeyspacesOp> {

    private final NBConfiguration cfg;
    private final DriverSpaceCache<? extends KeyspacesSpace> cache;

    public KeyspacesOpMapper(NBConfiguration cfg, DriverSpaceCache<? extends KeyspacesSpace> cache) {
        this.cfg = cfg;
        this.cache = cache;
    }

    @Override
    public OpDispenser<KeyspacesOp> apply(ParsedOp cmd) {
        String space = cmd.getStaticConfigOr("space", "default");
        KeyspacesClient keyspacesClient = cache.get(space).getKeyspacesClient();

        /*
         * If the user provides a body element, then they want to provide the JSON or
         * a data structure that can be converted into JSON, bypassing any further
         * specialized type-checking or op-type specific features
         */
        if (cmd.isDefined("body")) {
            throw new RuntimeException("This mode is reserved for later. Do not use the 'body' op field.");
//            return new RawKeyspacesOpDispenser(cmd);
        } else {
            TypeAndTarget<KeyspacesCmdType,String> cmdType = cmd.getTargetEnum(KeyspacesCmdType.class,String.class);
            return switch (cmdType.enumId) {
            	case CreateKeyspace -> new KeyspacesCreateKeyspaceOpDispenser(keyspacesClient, cmd, cmdType.targetFunction);
                //case CreateTable -> new DDBCreateTableOpDispenser(ddb, cmd, cmdType.targetFunction);
                //case DeleteTable -> new DDBDeleteTableOpDispenser(ddb, cmd, cmdType.targetFunction);
                //case PutItem -> new DDBPutItemOpDispenser(ddb, cmd, cmdType.targetFunction);
                //case GetItem -> new DDBGetItemOpDispenser(ddb, cmd, cmdType.targetFunction);
                //case Query -> new DDBQueryOpDispenser(ddb, cmd, cmdType.targetFunction);
            };
        }

    }

}
