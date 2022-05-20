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

import io.nosqlbench.adapter.keyspaces.optypes.KeyspacesCreateKeyspaceOp;
import io.nosqlbench.adapter.keyspaces.optypes.KeyspacesOp;
import io.nosqlbench.engine.api.activityimpl.BaseOpDispenser;
import io.nosqlbench.engine.api.templating.ParsedOp;
import software.amazon.awssdk.services.keyspaces.KeyspacesClient;
import software.amazon.awssdk.services.keyspaces.model.CreateKeyspaceRequest;
import software.amazon.awssdk.services.keyspaces.model.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

/**
 * <pre>{@code
 * Request Syntax
 * {
 *   "keyspaceName": "string",
 *   "tags": [ 
 *     { 
 *       "key": "string",
 *       "value": "string"
 *     }
 *   ]
 * }
 * }</pre>
 */
public class KeyspacesCreateKeyspaceOpDispenser extends BaseOpDispenser<KeyspacesOp> {

    private final KeyspacesClient keyspacesClient;
    private final LongFunction<String> keyspaceNameFunc;
//    private final LongFunction<Collection<Tag>> tagsFunc;

    public KeyspacesCreateKeyspaceOpDispenser(KeyspacesClient client, ParsedOp cmd, LongFunction<?> targetFunc) {
        super(cmd);
        this.keyspacesClient = client;
        this.keyspaceNameFunc = l -> targetFunc.apply(l).toString();
    }

    @Override
    public KeyspacesCreateKeyspaceOp apply(long cycle) {
    	CreateKeyspaceRequest rq = CreateKeyspaceRequest.builder()
    			.keyspaceName(keyspaceNameFunc.apply(cycle))
    			.build();
        return new KeyspacesCreateKeyspaceOp(keyspacesClient, rq);
    }

//    private LongFunction<Collection<Tag>> resolveTagsFunction(ParsedOp cmd) {
//        LongFunction<? extends List> tagsList = cmd.getAsRequiredFunction("Tags", List.class);
//
//        return (long l) -> {
//            List<Tag> tags = new ArrayList<>();
//            tagsList.apply(l).forEach((k, v) -> {
//            	
//                tags.add(new Tag(Tag.BuilderImpl k.toString(), KeyType.valueOf(v.toString())));
//            });
//            return tags;
//        };
//    }
}