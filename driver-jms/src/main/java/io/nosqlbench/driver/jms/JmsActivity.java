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

package io.nosqlbench.driver.jms;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import io.nosqlbench.driver.jms.conn.JmsConnInfo;
import io.nosqlbench.driver.jms.conn.JmsPulsarConnInfo;
import io.nosqlbench.driver.jms.ops.JmsOp;
import io.nosqlbench.driver.jms.util.JmsUtil;
import io.nosqlbench.driver.jms.util.PulsarConfig;
import io.nosqlbench.engine.api.activityapi.errorhandling.modular.NBErrorHandler;
import io.nosqlbench.engine.api.activityapi.planning.OpSequence;
import io.nosqlbench.engine.api.activityimpl.ActivityDef;
import io.nosqlbench.engine.api.activityimpl.OpDispenser;
import io.nosqlbench.engine.api.activityimpl.SimpleActivity;
import io.nosqlbench.engine.api.metrics.ActivityMetrics;
import org.apache.commons.lang3.StringUtils;

import javax.jms.Destination;
import javax.jms.JMSContext;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class JmsActivity extends SimpleActivity {

    private final ConcurrentHashMap<String, Destination> jmsDestinations = new ConcurrentHashMap<>();

    private String jmsProviderType;
    private JmsConnInfo jmsConnInfo;

    private JMSContext jmsContext;

    private OpSequence<OpDispenser<? extends JmsOp>> sequence;
    private volatile Throwable asyncOperationFailure;
    private NBErrorHandler errorhandler;

    private Timer bindTimer;
    private Timer executeTimer;
    private Counter bytesCounter;
    private Histogram messagesizeHistogram;

    public JmsActivity(ActivityDef activityDef) {
        super(activityDef);
    }

    @Override
    public void initActivity() {
        super.initActivity();

        // default JMS type: Pulsar
        // - currently this is the only supported JMS provider
        jmsProviderType =
            activityDef.getParams()
                .getOptionalString(JmsUtil.JMS_PROVIDER_TYPE_KEY_STR)
                .orElse(JmsUtil.JMS_PROVIDER_TYPES.PULSAR.label);

        // "Pulsar" as the JMS provider
        if (StringUtils.equalsIgnoreCase(jmsProviderType, JmsUtil.JMS_PROVIDER_TYPES.PULSAR.label )) {

            String webSvcUrl =
                activityDef.getParams()
                    .getOptionalString(JmsUtil.JMS_PULSAR_PROVIDER_WEB_URL_KEY_STR)
                    .orElse("http://localhost:8080");
            String pulsarSvcUrl =
                activityDef.getParams()
                    .getOptionalString(JmsUtil.JMS_PULSAR_PROVIDER_SVC_URL_KEY_STR)
                    .orElse("pulsar://localhost:6650");

            if (StringUtils.isAnyBlank(webSvcUrl, pulsarSvcUrl)) {
                throw new RuntimeException("For \"" + JmsUtil.JMS_PROVIDER_TYPES.PULSAR.label + "\" type, " +
                    "\"" + JmsUtil.JMS_PULSAR_PROVIDER_WEB_URL_KEY_STR + "\" and " +
                    "\"" + JmsUtil.JMS_PULSAR_PROVIDER_SVC_URL_KEY_STR  + "\" parameters are manadatory!");
            }

            // Check if extra Pulsar config. file is in place
            // - default file: "pulsar_config.properties" under the current directory
            String pulsarCfgFile =
                activityDef.getParams()
                    .getOptionalString(JmsUtil.JMS_PULSAR_PROVIDER_CFG_FILE_KEY_STR)
                    .orElse(JmsUtil.JMS_PULSAR_PROVIDER_DFT_CFG_FILE_NAME);

            PulsarConfig pulsarConfig = new PulsarConfig(pulsarCfgFile);

            jmsConnInfo = new JmsPulsarConnInfo(jmsProviderType, webSvcUrl, pulsarSvcUrl, pulsarConfig);
        }
        else {
            throw new RuntimeException("Unsupported JMS driver type : " + jmsProviderType);
        }

        PulsarConnectionFactory factory;
        factory = new PulsarConnectionFactory(jmsConnInfo.getJmsConnConfig());
        this.jmsContext = factory.createContext();

        bindTimer = ActivityMetrics.timer(activityDef, "bind", this.getHdrDigits());
        executeTimer = ActivityMetrics.timer(activityDef, "execute", this.getHdrDigits());
        bytesCounter = ActivityMetrics.counter(activityDef, "bytes");
        messagesizeHistogram = ActivityMetrics.histogram(activityDef, "messagesize", this.getHdrDigits());

        if (StringUtils.equalsIgnoreCase(jmsProviderType, JmsUtil.JMS_PROVIDER_TYPES.PULSAR.label )) {
            this.sequence = createOpSequence((ot) -> new ReadyPulsarJmsOp(ot, this), false, Optional.empty());
        }

        setDefaultsFromOpSequence(sequence);
        onActivityDefUpdate(activityDef);

        this.errorhandler = new NBErrorHandler(
            () -> activityDef.getParams().getOptionalString("errors").orElse("stop"),
            this::getExceptionMetrics
        );
    }

    private static String buildCacheKey(String... keyParts) {
        return String.join("::", keyParts);
    }

    /**
     * If the JMS destination that corresponds to a topic exists, reuse it; Otherwise, create it
     */
    public Destination getOrCreateJmsDestination(String jmsDestinationType, String destName) {
        String destinationCacheKey = buildCacheKey(jmsDestinationType, destName);
        Destination destination = jmsDestinations.get(destinationCacheKey);

        if ( destination == null ) {
            // TODO: should we match Persistent/Non-peristent JMS Delivery mode with
            //       Pulsar Persistent/Non-prsistent topic?
            if (StringUtils.equalsIgnoreCase(jmsDestinationType, JmsUtil.JMS_DESTINATION_TYPES.QUEUE.label)) {
                destination = jmsContext.createQueue(destName);
            } else if (StringUtils.equalsIgnoreCase(jmsDestinationType, JmsUtil.JMS_DESTINATION_TYPES.TOPIC.label)) {
                destination = jmsContext.createTopic(destName);
            }

            jmsDestinations.put(destinationCacheKey, destination);
        }

        return destination;
    }

    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) { super.onActivityDefUpdate(activityDef); }
    public OpSequence<OpDispenser<? extends JmsOp>> getSequencer() { return sequence; }

    public String getJmsProviderType() { return jmsProviderType; }
    public JmsConnInfo getJmsConnInfo() { return jmsConnInfo; }
    public JMSContext getJmsContext() { return jmsContext; }

    public Timer getBindTimer() { return bindTimer; }
    public Timer getExecuteTimer() { return this.executeTimer; }
    public Counter getBytesCounter() { return bytesCounter; }
    public Histogram getMessagesizeHistogram() { return messagesizeHistogram; }

    public NBErrorHandler getErrorhandler() { return errorhandler; }

    public void failOnAsyncOperationFailure() {
        if (asyncOperationFailure != null) {
            throw new RuntimeException(asyncOperationFailure);
        }
    }
    public void asyncOperationFailed(Throwable ex) {
        this.asyncOperationFailure = ex;
    }
}
