package io.nosqlbench.driver.pulsar;

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


import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import io.nosqlbench.driver.pulsar.ops.PulsarOp;
import io.nosqlbench.driver.pulsar.ops.ReadyPulsarOp;
import io.nosqlbench.driver.pulsar.util.PulsarActivityUtil;
import io.nosqlbench.driver.pulsar.util.PulsarNBClientConf;
import io.nosqlbench.engine.api.activityapi.core.ActivityDefObserver;
import io.nosqlbench.engine.api.activityapi.errorhandling.modular.NBErrorHandler;
import io.nosqlbench.engine.api.activityapi.planning.OpSequence;
import io.nosqlbench.engine.api.activityapi.ratelimits.RateLimiter;
import io.nosqlbench.engine.api.activityapi.ratelimits.RateLimiters;
import io.nosqlbench.engine.api.activityimpl.ActivityDef;
import io.nosqlbench.engine.api.activityimpl.OpDispenser;
import io.nosqlbench.engine.api.activityimpl.SimpleActivity;
import io.nosqlbench.engine.api.metrics.ActivityMetrics;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.common.schema.KeyValueEncodingType;

import java.util.Map;

public class PulsarActivity extends SimpleActivity implements ActivityDefObserver {

    private final static Logger logger = LogManager.getLogger(PulsarActivity.class);

    private Counter bytesCounter;
    private Histogram messageSizeHistogram;
    private Timer bindTimer;
    private Timer executeTimer;
    private Timer createTransactionTimer;
    private Timer commitTransactionTimer;

    // Metrics for NB Pulsar driver milestone: https://github.com/nosqlbench/nosqlbench/milestone/11
    // - end-to-end latency
    private Histogram e2eMsgProcLatencyHistogram;

    /**
     * A histogram that tracks payload round-trip-time, based on a user-defined field in some sender
     * system which can be interpreted as millisecond epoch time in the system's local time zone.
     * This is paired with a field name of the same type to be extracted and reported in a meteric
     * named 'payload-rtt'.
     */
    private Histogram payloadRttHistogram;

    // - message out of sequence error counter
    private Counter msgErrOutOfSeqCounter;
    // - message loss counter
    private Counter msgErrLossCounter;
    // - message duplicate (when dedup is enabled) error counter
    private Counter msgErrDuplicateCounter;

    private PulsarSpaceCache pulsarCache;

    private PulsarNBClientConf pulsarNBClientConf;
    private String pulsarSvcUrl;
    private String webSvcUrl;
    private PulsarAdmin pulsarAdmin;
    private PulsarClient pulsarClient;
    private Schema<?> pulsarSchema;

    private NBErrorHandler errorHandler;
    private OpSequence<OpDispenser<PulsarOp>> sequencer;
    private volatile Throwable asyncOperationFailure;
    private boolean cycleratePerThread;

    public PulsarActivity(ActivityDef activityDef) {
        super(activityDef);
    }

    @Override
    public void shutdownActivity() {
        super.shutdownActivity();

        if (pulsarCache == null) {
            return;
        }

        for (PulsarSpace pulsarSpace : pulsarCache.getAssociatedPulsarSpace()) {
            pulsarSpace.shutdownPulsarSpace();
        }
    }

    @Override
    public void initActivity() {
        super.initActivity();
        pulsarCache = new PulsarSpaceCache(this);

        bytesCounter = ActivityMetrics.counter(activityDef, "bytes");
        messageSizeHistogram = ActivityMetrics.histogram(activityDef, "message_size");
        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");
        createTransactionTimer = ActivityMetrics.timer(activityDef, "create_transaction");
        commitTransactionTimer = ActivityMetrics.timer(activityDef, "commit_transaction");

        e2eMsgProcLatencyHistogram = ActivityMetrics.histogram(activityDef, "e2e_msg_latency");
        payloadRttHistogram = ActivityMetrics.histogram(activityDef, "payload_rtt");

        msgErrOutOfSeqCounter = ActivityMetrics.counter(activityDef, "err_msg_oos");
        msgErrLossCounter = ActivityMetrics.counter(activityDef, "err_msg_loss");
        msgErrDuplicateCounter = ActivityMetrics.counter(activityDef, "err_msg_dup");

        String pulsarClntConfFile =
            activityDef.getParams().getOptionalString("config").orElse("config.properties");
        pulsarNBClientConf = new PulsarNBClientConf(pulsarClntConfFile);

        pulsarSvcUrl =
            activityDef.getParams().getOptionalString("service_url").orElse("pulsar://localhost:6650");
        webSvcUrl =
            activityDef.getParams().getOptionalString("web_url").orElse("http://localhost:8080");

        initPulsarAdminAndClientObj();
        createPulsarSchemaFromConf();


        this.sequencer = createOpSequence((ot) -> new ReadyPulsarOp(ot, pulsarCache, this));
        setDefaultsFromOpSequence(sequencer);
        onActivityDefUpdate(activityDef);

        this.errorHandler = new NBErrorHandler(
            () -> activityDef.getParams().getOptionalString("errors").orElse("stop"),
            this::getExceptionMetrics
        );

        cycleratePerThread = activityDef.getParams().takeBoolOrDefault("cyclerate_per_thread", false);
    }

    private final ThreadLocal<RateLimiter> cycleLimiterThreadLocal = ThreadLocal.withInitial(() -> {
        if (super.getCycleLimiter() != null) {
            return RateLimiters.createOrUpdate(this.getActivityDef(), "cycles", null,
                super.getCycleLimiter().getRateSpec());
        } else {
            return null;
        }
    });

    @Override
    public RateLimiter getCycleLimiter() {
        if (cycleratePerThread) {
            return cycleLimiterThreadLocal.get();
        } else {
            return super.getCycleLimiter();
        }
    }

    public NBErrorHandler getErrorHandler() { return errorHandler; }

    public OpSequence<OpDispenser<PulsarOp>> getSequencer() { return sequencer; }

    public void failOnAsyncOperationFailure() {
        if (asyncOperationFailure != null) {
            throw new RuntimeException(asyncOperationFailure);
        }
    }

    public void asyncOperationFailed(Throwable ex) {
        this.asyncOperationFailure = ex;
    }

    /**
     * Initialize
     * - PulsarAdmin object for adding/deleting tenant, namespace, and topic
     * - PulsarClient object for message publishing and consuming
     */
    private void initPulsarAdminAndClientObj() {
        PulsarAdminBuilder adminBuilder =
            PulsarAdmin.builder()
                .serviceHttpUrl(webSvcUrl);

        ClientBuilder clientBuilder = PulsarClient.builder();

        try {
            Map<String, Object> clientConfMap = pulsarNBClientConf.getClientConfMap();

            // Override "client.serviceUrl" setting in config.properties
            clientConfMap.remove("serviceUrl");
            clientBuilder.loadConf(clientConfMap).serviceUrl(pulsarSvcUrl);

            // Pulsar Authentication
            String authPluginClassName =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.authPulginClassName.label);
            String authParams =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.authParams.label);

            if ( !StringUtils.isAnyBlank(authPluginClassName, authParams) ) {
                adminBuilder.authentication(authPluginClassName, authParams);
                clientBuilder.authentication(authPluginClassName, authParams);
            }

            String useTlsStr =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.useTls.label);
            boolean useTls = BooleanUtils.toBoolean(useTlsStr);

            String tlsTrustCertsFilePath =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.tlsTrustCertsFilePath.label);

            String tlsAllowInsecureConnectionStr =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.tlsAllowInsecureConnection.label);
            boolean tlsAllowInsecureConnection = BooleanUtils.toBoolean(tlsAllowInsecureConnectionStr);

            String tlsHostnameVerificationEnableStr =
                (String) pulsarNBClientConf.getClientConfValue(PulsarActivityUtil.CLNT_CONF_KEY.tlsHostnameVerificationEnable.label);
            boolean tlsHostnameVerificationEnable = BooleanUtils.toBoolean(tlsHostnameVerificationEnableStr);

            if ( useTls ) {
                adminBuilder
                    .enableTlsHostnameVerification(tlsHostnameVerificationEnable);

                clientBuilder
                    .enableTlsHostnameVerification(tlsHostnameVerificationEnable);

                if (!StringUtils.isBlank(tlsTrustCertsFilePath)) {
                    adminBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
                    clientBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
                }
            }

            // Put this outside "if (useTls)" block for easier handling of "tlsAllowInsecureConnection"
            adminBuilder.allowTlsInsecureConnection(tlsAllowInsecureConnection);
            clientBuilder.allowTlsInsecureConnection(tlsAllowInsecureConnection);

            pulsarAdmin = adminBuilder.build();
            pulsarClient = clientBuilder.build();

            ////////////////
            // Not supported in Pulsar 2.8.0
            //
            // ClientConfigurationData configurationData = pulsarAdmin.getClientConfigData();
            // logger.debug(configurationData.toString());

        } catch (PulsarClientException e) {
            logger.error("Fail to create PulsarAdmin and/or PulsarClient object from the global configuration!");
            throw new RuntimeException("Fail to create PulsarAdmin and/or PulsarClient object from global configuration!");
        }
    }

    /**
     * Get Pulsar schema from the definition string
     */
    private void createPulsarSchemaFromConf() {
        pulsarSchema = buldSchemaFromDefinition("schema.type", "schema.definition");

        // this is to allow KEY_VALUE schema
        if (pulsarNBClientConf.hasSchemaConfKey("schema.key.type")) {
           Schema<?> pulsarKeySchema = buldSchemaFromDefinition("schema.key.type", "schema.key.definition");
           Object encodingType =  pulsarNBClientConf.getSchemaConfValue("schema.keyvalue.encodingtype");
           KeyValueEncodingType keyValueEncodingType = KeyValueEncodingType.SEPARATED;
           if (encodingType != null) {
               keyValueEncodingType = KeyValueEncodingType.valueOf(encodingType.toString());
           }
           pulsarSchema = Schema.KeyValue(pulsarKeySchema, pulsarSchema, keyValueEncodingType);
        }
    }

    private Schema<?> buldSchemaFromDefinition(String schemaTypeConfEntry,
                                                      String schemaDefinitionConfEntry) {
        Object value = pulsarNBClientConf.getSchemaConfValue(schemaTypeConfEntry);
        Object schemaDefinition = pulsarNBClientConf.getSchemaConfValue(schemaDefinitionConfEntry);
        String schemaType = (value != null) ? value.toString() : "";

        Schema<?> result;
        if (PulsarActivityUtil.isAvroSchemaTypeStr(schemaType)) {
            String schemaDefStr = (schemaDefinition != null) ? schemaDefinition.toString() : "";
            result = PulsarActivityUtil.getAvroSchema(schemaType, schemaDefStr);
        } else if (PulsarActivityUtil.isPrimitiveSchemaTypeStr(schemaType)) {
            result = PulsarActivityUtil.getPrimitiveTypeSchema(schemaType);
        } else if (PulsarActivityUtil.isAutoConsumeSchemaTypeStr(schemaType)) {
            result = Schema.AUTO_CONSUME();
        } else {
            throw new RuntimeException("Unsupported schema type string: " + schemaType + "; " +
                "Only primitive type, Avro type and AUTO_CONSUME are supported at the moment!");
        }
        return result;
    }

    public PulsarNBClientConf getPulsarConf() { return this.pulsarNBClientConf;}
    public String getPulsarSvcUrl() { return this.pulsarSvcUrl;}
    public String getWebSvcUrl() { return this.webSvcUrl; }
    public PulsarAdmin getPulsarAdmin() { return this.pulsarAdmin; }
    public PulsarClient getPulsarClient() { return this.pulsarClient; }
    public Schema<?> getPulsarSchema() { return pulsarSchema; }

    public Counter getBytesCounter() { return bytesCounter; }
    public Histogram getMessageSizeHistogram() { return messageSizeHistogram; }
    public Timer getBindTimer() { return bindTimer; }
    public Timer getExecuteTimer() { return this.executeTimer; }
    public Timer getCreateTransactionTimer() { return createTransactionTimer; }
    public Timer getCommitTransactionTimer() { return commitTransactionTimer; }

    public Histogram getPayloadRttHistogram() {return payloadRttHistogram;}
    public Histogram getE2eMsgProcLatencyHistogram() { return e2eMsgProcLatencyHistogram; }
    public Counter getMsgErrOutOfSeqCounter() { return msgErrOutOfSeqCounter; }
    public Counter getMsgErrLossCounter() { return msgErrLossCounter; }
    public Counter getMsgErrDuplicateCounter() { return msgErrDuplicateCounter; }
}
