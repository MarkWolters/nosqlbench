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

import io.nosqlbench.adapter.keyspaces.optypes.KeyspacesOp;
import io.nosqlbench.engine.api.activityimpl.OpMapper;
import io.nosqlbench.engine.api.activityimpl.uniform.BaseDriverAdapter;
import io.nosqlbench.engine.api.activityimpl.uniform.DriverAdapter;
import io.nosqlbench.engine.api.activityimpl.uniform.DriverSpaceCache;
import io.nosqlbench.nb.annotations.Maturity;
import io.nosqlbench.nb.annotations.Service;
import io.nosqlbench.nb.api.config.standard.NBConfigModel;
import io.nosqlbench.nb.api.config.standard.NBConfiguration;

import java.util.function.Function;

@Service(value = DriverAdapter.class, selector = "keyspaces", maturity = Maturity.Experimental)
public class KeyspacesDriverAdapter extends BaseDriverAdapter<KeyspacesOp, KeyspacesSpace> {

    @Override
    public OpMapper<KeyspacesOp> getOpMapper() {
        DriverSpaceCache<? extends KeyspacesSpace> spaceCache = getSpaceCache();
        NBConfiguration adapterConfig = getConfiguration();
        return new KeyspacesOpMapper(adapterConfig, spaceCache);
    }

    @Override
    public Function<String, ? extends KeyspacesSpace> getSpaceInitializer(NBConfiguration cfg) {
        return (s) -> new KeyspacesSpace(s,cfg);
    }

    @Override
    public NBConfigModel getConfigModel() {
        return super.getConfigModel().add(KeyspacesSpace.getConfigModel());
    }
}
