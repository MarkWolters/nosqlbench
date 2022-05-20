package io.nosqlbench.driver.direct.optypes;

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


import io.nosqlbench.virtdata.library.basics.core.threadstate.SharedState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;

public class DynamicMethodCall implements Runnable {

    private final static Logger logger = LogManager.getLogger(DynamicMethodCall.class);

    private final Map<String, Object> callinfo;

    public DynamicMethodCall(Map<String,Object> callinfo) {
        this.callinfo = callinfo;
    }

    // At this point, class and method should have been set, and args optionally
    private void callMethod() {
        String className = callinfo.get("class").toString();
        String methodName = callinfo.get("method").toString();

        Class<?> clazz;
        Method method;
        try {
            clazz = Class.forName(className);
            method = clazz.getMethod(methodName);

            Object instance = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                if (callinfo.containsKey("instance")) {
                    String instanceName = callinfo.get("instance").toString();
                    instance = SharedState.tl_ObjectMap.get().get(instanceName);
                }
            }

            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < args.length; i++) {
                String posname = "arg" + i;
                if (callinfo.containsKey(posname)) {
                    args[i] = callinfo.get(posname);
                } else if (parameters[i].isNamePresent()) {
                    String argname = parameters[i].getName();
                    if (callinfo.containsKey(argname)) {
                        args[i]=callinfo.get(argname);
                    } else {
                        throw new RuntimeException("could not find arg named '" + posname + "', nor '" + argname + "' in op template for method " + method.toGenericString());
                    }
                }
            }
            Object result = method.invoke(instance, args);

            if (callinfo.containsKey("save")) {
                String saveAs = callinfo.get("save").toString();
                SharedState.tl_ObjectMap.get().put(saveAs,result);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void run() {
        callMethod();
    }
}
