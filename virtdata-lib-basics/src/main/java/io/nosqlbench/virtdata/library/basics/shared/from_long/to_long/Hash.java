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

package io.nosqlbench.virtdata.library.basics.shared.from_long.to_long;

import io.nosqlbench.virtdata.annotations.types.Categories;
import io.nosqlbench.virtdata.annotations.types.Category;
import io.nosqlbench.virtdata.annotations.types.ThreadSafeMapper;
import io.nosqlbench.virtdata.library.basics.murmur.Murmur3F;

import java.nio.ByteBuffer;
import java.util.function.LongUnaryOperator;

/**
 * This uses the Murmur3F (64-bit optimized) version of Murmur3,
 * not as a checksum, but as a simple hash. It doesn't bother
 * pushing the high-64 bits of input, since it only uses the lower
 * 64 bits of output. It does, however, return the absolute value.
 * This is to make it play nice with users and other libraries.
 */
@ThreadSafeMapper
@Categories({Category.general, Category.general})
public class Hash implements LongUnaryOperator {

    private final transient ThreadLocal<Murmur3F> murmur3f_TL = ThreadLocal.withInitial(Murmur3F::new);

    @Override
    public long applyAsLong(long value) {
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
        Murmur3F murmur3f = murmur3f_TL.get();
        murmur3f.reset();
        bb.putLong(0,value);
        murmur3f.update(bb.array(),0,Long.BYTES);
        long result= Math.abs(murmur3f.getValue());
        return result;
    }
}
