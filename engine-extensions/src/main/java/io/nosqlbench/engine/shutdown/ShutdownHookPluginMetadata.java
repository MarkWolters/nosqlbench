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

package io.nosqlbench.engine.shutdown;

import com.codahale.metrics.MetricRegistry;
import io.nosqlbench.engine.api.extensions.ScriptingPluginInfo;
import io.nosqlbench.nb.annotations.types.Selector;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptContext;

@Selector("shutdown")
public class ShutdownHookPluginMetadata implements ScriptingPluginInfo<ShutdownHookPlugin> {

    @Override
    public String getDescription() {
        return "Register shutdown hooks in the form of javascript functions.";
    }

    @Override
    public ShutdownHookPlugin getExtensionObject(Logger logger, MetricRegistry metricRegistry, ScriptContext scriptContext) {
        return new ShutdownHookPlugin(logger,metricRegistry,scriptContext);
    }
}
