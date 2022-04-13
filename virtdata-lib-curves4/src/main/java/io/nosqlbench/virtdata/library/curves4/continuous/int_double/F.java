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

package io.nosqlbench.virtdata.library.curves4.continuous.int_double;

import io.nosqlbench.virtdata.annotations.types.Categories;
import io.nosqlbench.virtdata.annotations.types.Category;
import io.nosqlbench.virtdata.annotations.types.ThreadSafeMapper;
import org.apache.commons.statistics.distribution.FDistribution;

/**
 * @see <a href="https://en.wikipedia.org/wiki/F-distribution">Wikipedia: F-distribution</a>
 *
 * @see <a href="https://commons.apache.org/proper/commons-statistics/commons-statistics-distribution/apidocs/org/apache/commons/statistics/distribution/FDistribution.html">Commons JavaDoc: FDistribution</a>
 *
 * @see <a href="http://mathworld.wolfram.com/F-Distribution.html">Mathworld: F-Distribution</a>
 *
 * {@inheritDoc}
 */
@ThreadSafeMapper
@Categories({Category.distributions})
public class F extends IntToDoubleContinuousCurve {
    public F(double numeratorDegreesOfFreedom, double denominatorDegreesOfFreedom, String... mods) {
        super(new FDistribution(numeratorDegreesOfFreedom, denominatorDegreesOfFreedom), mods);
    }
}
