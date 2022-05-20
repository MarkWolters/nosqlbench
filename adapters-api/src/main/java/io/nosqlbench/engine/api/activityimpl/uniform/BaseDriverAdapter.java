package io.nosqlbench.engine.api.activityimpl.uniform;

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


import io.nosqlbench.engine.api.activityimpl.uniform.fieldmappers.FieldDestructuringMapper;
import io.nosqlbench.engine.api.activityimpl.uniform.flowtypes.Op;
import io.nosqlbench.nb.api.config.standard.ConfigModel;
import io.nosqlbench.nb.api.config.standard.NBConfigModel;
import io.nosqlbench.nb.api.config.standard.NBConfigurable;
import io.nosqlbench.nb.api.config.standard.NBConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseDriverAdapter<R extends Op,S>
    implements DriverAdapter<R,S>, NBConfigurable {

    private final DriverSpaceCache<? extends S> spaceCache;
    private NBConfiguration cfg;

    protected BaseDriverAdapter() {
        this.spaceCache = new DriverSpaceCache<>(getSpaceInitializer(getConfiguration()));
    }

    /**
     * BaseDriverAdapter will take any provided functions from {@link #getOpStmtRemappers()}
     * and {@link #getOpFieldRemappers()} and construct a preprocessor list. These are applied
     * successively to the op template fields so long as remapping occurs.
     * @return a list of preprocessors for op template fields
     */
    @Override
    public final Function<Map<String, Object>, Map<String, Object>> getPreprocessor() {
        List<Function<Map<String,Object>,Map<String,Object>>> mappers = new ArrayList<>();
        List<Function<Map<String,Object>,Map<String,Object>>> stmtRemappers =
            getOpStmtRemappers().stream()
                .map(m -> new FieldDestructuringMapper("stmt",m))
                .collect(Collectors.toList());
        mappers.addAll(stmtRemappers);
        mappers.addAll(getOpFieldRemappers());

        if (mappers.size()==0) {
            return (i) -> i;
        }

        Function<Map<String,Object>,Map<String,Object>> remapper = null;
        for (int i = 0; i < mappers.size(); i++) {
            if (i==0) {
                remapper=mappers.get(i);
            } else {
                remapper = remapper.andThen(mappers.get(i));
            }
        }

        return remapper;
    }

    /**
     * <p>Provide a list of field remappers which operate exclusively on the 'stmt' field
     * in the op template. These are useful, for example, for taking the 'stmt' field
     * and parsing it into component fields which might otherwise be specified by the user.
     * This allows users to specify String-form op templates which are automatically
     * destructured into the canonical field-wise form for a given type of operation.</p>
     * <br/>
     * <p>Each function in this list is applied in order. If the function returns a value,
     * then the 'stmt' field is removed and the resulting map is added to the other
     * fields in the op template.</p>
     *
     * @return A list of optionally applied remapping functions.
     */
    public List<Function<String, Optional<Map<String,Object>>>> getOpStmtRemappers() {
        return List.of();
    }

    /**
     * <p>Provide a list of field remappers which operate on arbitrary fields.
     * Each function is applied to the op template fields. </p>
     * @return
     */
    public List<Function<Map<String,Object>,Map<String,Object>>> getOpFieldRemappers() {
        return List.of();
    }

    @Override
    public final DriverSpaceCache<? extends S> getSpaceCache() {
        return spaceCache;
    }

    /**
     * In order to be provided with config information, it is required
     * that the driver adapter specify the valid configuration options,
     * their types, and so on.
     */
    @Override
    public NBConfigModel getConfigModel() {
        return ConfigModel.of(this.getClass());
    }

    @Override
    public NBConfiguration getConfiguration() {
        return cfg;
    }

    @Override
    public void applyConfig(NBConfiguration cfg) {
        this.cfg = cfg;
    }

}
