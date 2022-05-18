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

package io.nosqlbench.adapters.stdout;

import io.nosqlbench.adapters.api.activityconfig.yaml.OpTemplate;
import io.nosqlbench.adapters.api.opmapping.OpMapper;
import io.nosqlbench.adapters.api.opmapping.uniform.BaseDriverAdapter;
import io.nosqlbench.adapters.api.opmapping.uniform.DriverSpaceCache;
import io.nosqlbench.adapters.api.templating.OpTemplateSupplier;
import io.nosqlbench.api.config.standard.ConfigModel;
import io.nosqlbench.api.config.standard.NBConfigModel;
import io.nosqlbench.api.config.standard.NBConfiguration;
import io.nosqlbench.nb.annotations.types.Selector;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Selector("stdout")
public class StdoutDriverAdapter extends BaseDriverAdapter<StdoutOp, StdoutSpace> implements OpTemplateSupplier {

    @Override
    public OpMapper<StdoutOp> getOpMapper() {
        DriverSpaceCache<? extends StdoutSpace> ctxCache = getSpaceCache();
        return new StdoutOpMapper(ctxCache);
    }

    @Override
    public Function<String, ? extends StdoutSpace> getSpaceInitializer(NBConfiguration cfg) {
        return (s) -> new StdoutSpace(cfg);
    }

    @Override
    public NBConfigModel getConfigModel() {
        return ConfigModel.of(this.getClass())
            .add(super.getConfigModel())
            .add(StdoutSpace.getConfigModel());
    }

    @Override
    public Optional<List<OpTemplate>> loadOpTemplates(NBConfiguration cfg) {
        throw new RuntimeException("implement me");
    }
}
