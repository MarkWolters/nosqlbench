package io.nosqlbench.driver.jms;

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


import io.nosqlbench.driver.jms.ops.JmsOp;
import io.nosqlbench.driver.jms.util.JmsUtil;
import io.nosqlbench.engine.api.activityconfig.yaml.OpTemplate;
import io.nosqlbench.engine.api.activityimpl.OpDispenser;
import io.nosqlbench.engine.api.templating.CommandTemplate;
import org.apache.commons.lang3.BooleanUtils;

import java.util.function.LongFunction;

abstract public class ReadyJmsOp implements OpDispenser<JmsOp> {

    protected final OpTemplate optpl;
    protected final CommandTemplate cmdTpl;
    protected final JmsActivity jmsActivity;

    protected final String stmtOpType;
    protected LongFunction<Boolean> asyncApiFunc;
    protected LongFunction<String> jmsDestinationTypeFunc;

    protected final LongFunction<JmsOp> opFunc;

    public ReadyJmsOp(OpTemplate opTemplate, JmsActivity jmsActivity) {
        this.optpl = opTemplate;
        this.cmdTpl = new CommandTemplate(optpl);
        this.jmsActivity = jmsActivity;

        if (!cmdTpl.containsKey("optype") || !cmdTpl.isStatic("optype")) {
            throw new RuntimeException("Statement parameter \"optype\" must be static and have a valid value!");
        }
        this.stmtOpType = cmdTpl.getStatic("optype");

        // Global/Doc-level parameter: async_api
        if (cmdTpl.containsKey(JmsUtil.ASYNC_API_KEY_STR)) {
            if (cmdTpl.isStatic(JmsUtil.ASYNC_API_KEY_STR)) {
                boolean value = BooleanUtils.toBoolean(cmdTpl.getStatic(JmsUtil.ASYNC_API_KEY_STR));
                this.asyncApiFunc = (l) -> value;
            } else {
                throw new RuntimeException("\"" + JmsUtil.ASYNC_API_KEY_STR + "\" parameter cannot be dynamic!");
            }
        }

        // Global/Doc-level parameter: jms_desitation_type
        // - queue: point-to-point
        // - topic: pub/sub
        if (cmdTpl.containsKey(JmsUtil.JMS_DESTINATION_TYPE_KEY_STR)) {
            if (cmdTpl.isStatic(JmsUtil.JMS_DESTINATION_TYPE_KEY_STR)) {
                jmsDestinationTypeFunc = (l) -> cmdTpl.getStatic(JmsUtil.JMS_DESTINATION_TYPE_KEY_STR);
            } else {
                throw new RuntimeException("\"" + JmsUtil.JMS_DESTINATION_TYPE_KEY_STR + "\" parameter cannot be dynamic!");
            }
        }

        this.opFunc = resolveJms();
    }

    public JmsOp apply(long value) { return opFunc.apply(value); }

    abstract LongFunction<JmsOp> resolveJms();
}
