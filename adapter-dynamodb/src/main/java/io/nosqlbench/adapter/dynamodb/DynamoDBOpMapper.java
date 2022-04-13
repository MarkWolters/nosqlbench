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

package io.nosqlbench.adapter.dynamodb;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import io.nosqlbench.adapter.dynamodb.opdispensers.*;
import io.nosqlbench.adapter.dynamodb.optypes.DynamoDBOp;
import io.nosqlbench.adapters.api.opmapping.OpDispenser;
import io.nosqlbench.adapters.api.opmapping.OpMapper;
import io.nosqlbench.adapters.api.opmapping.uniform.DriverSpaceCache;
import io.nosqlbench.adapters.api.templating.ParsedOp;
import io.nosqlbench.api.config.standard.NBConfiguration;
import io.nosqlbench.virtdata.api.templating.TypeAndTarget;

public class DynamoDBOpMapper implements OpMapper<DynamoDBOp> {

    private final NBConfiguration cfg;
    private final DriverSpaceCache<? extends DynamoDBSpace> cache;

    public DynamoDBOpMapper(NBConfiguration cfg, DriverSpaceCache<? extends DynamoDBSpace> cache) {
        this.cfg = cfg;
        this.cache = cache;
    }

    @Override
    public OpDispenser<DynamoDBOp> apply(ParsedOp cmd) {
        String space = cmd.getStaticConfigOr("space", "default");
        DynamoDB ddb = cache.get(space).getDynamoDB();

        /*
         * If the user provides a body element, then they want to provide the JSON or
         * a data structure that can be converted into JSON, bypassing any further
         * specialized type-checking or op-type specific features
         */
        if (cmd.isDefined("body")) {
            throw new RuntimeException("This mode is reserved for later. Do not use the 'body' op field.");
//            return new RawDynamoDBOpDispenser(cmd);
        } else {
            TypeAndTarget<DynamoDBCmdType,String> cmdType = cmd.getTargetEnum(DynamoDBCmdType.class,String.class);
            return switch (cmdType.enumId) {
                case CreateTable -> new DDBCreateTableOpDispenser(ddb, cmd, cmdType.targetFunction);
                case DeleteTable -> new DDBDeleteTableOpDispenser(ddb, cmd, cmdType.targetFunction);
                case PutItem -> new DDBPutItemOpDispenser(ddb, cmd, cmdType.targetFunction);
                case GetItem -> new DDBGetItemOpDispenser(ddb, cmd, cmdType.targetFunction);
                case Query -> new DDBQueryOpDispenser(ddb, cmd, cmdType.targetFunction);
            };
        }

    }

}
