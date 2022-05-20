package io.nosqlbench.activitytype.cql.datamappers.functions.to_tuple;

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


import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.TupleType;
import com.datastax.driver.core.TupleValue;
import io.nosqlbench.nb.api.config.standard.ConfigModel;
import io.nosqlbench.nb.api.config.standard.NBConfigModel;
import io.nosqlbench.nb.api.config.standard.NBMapConfigurable;
import io.nosqlbench.nb.api.config.standard.Param;
import io.nosqlbench.virtdata.api.annotations.Categories;
import io.nosqlbench.virtdata.api.annotations.Category;
import io.nosqlbench.virtdata.api.annotations.ThreadSafeMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

/**
 * Temporary function to test a specific nested type. This should be replaced
 * with a general custom/tuple type aware binding function.
 * The type supported is a CQL type: {@code map<text, frozen<tuple<int, bigint>>}
 *
 * Functions are required for:
 * <LI>
 * <LI>map size {@code (LongToIntFunction)}</LI>
 * <LI>key {@code (LongFunction<Object>)}</LI>
 * <LI>tuple field 1 {@code (LongToIntFunction)}</LI>
 * <LI>tuple field 2 {@code {LongToIntFunction)}</LI>
 * </LI>
 */
@ThreadSafeMapper
@Categories({Category.experimental})
public class CustomFunc955 implements LongFunction<Map<?, ?>>, NBMapConfigurable {

    private final LongToIntFunction sizefunc;
    private final LongFunction<Object> keyfunc;
    private final LongToIntFunction field1func;
    private final LongUnaryOperator field2func;
    private Cluster cluster;
    private TupleType tupleType;

    public CustomFunc955(LongToIntFunction sizefunc, LongFunction<Object> keyfunc,
                         LongToIntFunction field1func, LongToIntFunction field2func) {

        this.sizefunc = sizefunc;
        this.keyfunc = keyfunc;
        this.field1func = field1func;
        this.field2func = field2func::applyAsInt;
    }

    public CustomFunc955(LongToIntFunction sizefunc, LongFunction<Object> keyfunc,
                         LongToIntFunction field1func, LongUnaryOperator field2func) {

        this.sizefunc = sizefunc;
        this.keyfunc = keyfunc;
        this.field1func = field1func;
        this.field2func = field2func;
    }

    @Override
    public Map<?, ?> apply(long value) {
        int size = sizefunc.applyAsInt(value);

        HashMap<String, TupleValue> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = keyfunc.apply(value + i).toString();
            int tuple1 = field1func.applyAsInt(value + i);
            long tuple2 = field2func.applyAsLong(value + i);
            TupleValue tupleValue = tupleType.newValue(tuple1, tuple2);
            map.put(key, tupleValue);
        }
        return map;
    }

    @Override
    public void applyConfig(Map<String, ?> providedConfig) {
        this.cluster = Optional.ofNullable(providedConfig.get("cluster"))
            .map(Cluster.class::cast)
            .orElseThrow();
        this.tupleType = cluster.getMetadata().newTupleType(DataType.cint(), DataType.bigint());
    }

    @Override
    public NBConfigModel getConfigModel() {
        return ConfigModel.of(this.getClass())
            .add(Param.optional("<cluster>", Cluster.class))
            .asReadOnly();
    }
}
