package io.nosqlbench.engine.core.experimental;

/*
 * Copyright (c) 2022 nosqlbench
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableTests {

    @Test
    public void testCompletionStages() {
        CompletableFuture<Object> f = new CompletableFuture<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletableFuture<Object> objectCompletableFuture = f.completeAsync(() -> "foo", executorService);
        boolean bar = objectCompletableFuture.complete("bar");

    }
}
