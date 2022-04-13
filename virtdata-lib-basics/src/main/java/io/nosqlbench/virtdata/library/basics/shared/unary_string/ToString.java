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

package io.nosqlbench.virtdata.library.basics.shared.unary_string;

import io.nosqlbench.virtdata.annotations.types.ThreadSafeMapper;

import java.util.function.Function;

/**
 * Converts the input to the most obvious string representation with String.valueOf(...).
 * Forms which accept a function will evaluate that function first and then apply
 * String.valueOf() to the result.
 */
@ThreadSafeMapper
public class ToString implements Function<Object,String> {

    @Override
    public String apply(Object o) {
        return String.valueOf(o);
    }
}
