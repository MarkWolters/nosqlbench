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

package io.nosqlbench.engine.api.activityimpl.uniform;

import io.nosqlbench.engine.api.activityapi.core.ActionDispenser;
import io.nosqlbench.engine.api.activityapi.core.ActivityType;
import io.nosqlbench.api.activityimpl.ActivityDef;
import io.nosqlbench.engine.api.activityimpl.SimpleActivity;
import io.nosqlbench.adapters.api.opmapping.uniform.DriverAdapter;

public class StandardActivityType<A extends StandardActivity<?,?>> extends SimpleActivity implements ActivityType<A> {

    private final DriverAdapter<?,?> adapter;

    public StandardActivityType(DriverAdapter<?,?> adapter, ActivityDef activityDef) {
        super(activityDef);
        this.adapter = adapter;
        if (adapter instanceof ActivityDefAware) {
            ((ActivityDefAware) adapter).setActivityDef(activityDef);
        }
    }

    @Override
    public A getActivity(ActivityDef activityDef) {
        if (activityDef.getParams().getOptionalString("async").isPresent()) {
            throw new RuntimeException("This driver does not support async mode yet.");
        }

        return (A) new StandardActivity(adapter,activityDef);
    }

    @Override
    public ActionDispenser getActionDispenser(A activity) {
        return new StandardActionDispenser(activity);
    }


}
