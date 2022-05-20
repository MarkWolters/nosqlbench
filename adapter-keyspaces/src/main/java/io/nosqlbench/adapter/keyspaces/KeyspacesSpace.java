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

package io.nosqlbench.adapter.keyspaces;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;

import io.nosqlbench.nb.api.config.standard.*;
import io.nosqlbench.nb.api.errors.OpConfigError;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.keyspaces.KeyspacesClient;
import software.amazon.awssdk.services.keyspaces.KeyspacesClientBuilder;

import java.util.Optional;

public class KeyspacesSpace {
    private final String name;
    KeyspacesClient keyspacesClient;

    public KeyspacesSpace(String name, NBConfiguration cfg) {
        this.name = name;
        keyspacesClient = createClient(cfg);
    }

    public KeyspacesClient getKeyspacesClient() {
        return keyspacesClient;
    }

    private KeyspacesClient createClient(NBConfiguration cfg) {
        KeyspacesClientBuilder builder = KeyspacesClient.builder();
        Optional<String> region = cfg.getOptional("region");
        Optional<String> endpoint = cfg.getOptional("endpoint");
        Optional<String> signing_region = cfg.getOptional("signing_region");

        if (region.isPresent() && (endpoint.isPresent() || signing_region.isPresent())) {
            throw new OpConfigError("If you specify region, endpoint and signing_region options are not allowed");
        }

        if (region.isPresent()) {
            builder.region(Region.of(region.get()));
        } /*else if (endpoint.isPresent() && signing_region.isPresent()){
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint.get(), signing_region.get());
            builder = builder.withEndpointConfiguration(endpointConfiguration);
        }*/ else {
            throw new OpConfigError("Either region or endpoint and signing_region options are required.");
        }


        ClientConfiguration ccfg = new ClientConfiguration();
        cfg.getOptional("client_socket_timeout").map(Integer::parseInt).ifPresent(ccfg::withSocketTimeout);
        cfg.getOptional("client_execution_timeout").map(Integer::parseInt).ifPresent(ccfg::withClientExecutionTimeout);
        cfg.getOptional("client_max_connections").map(Integer::parseInt).ifPresent(ccfg::withMaxConnections);
        cfg.getOptional("client_max_error_retry").map(Integer::parseInt).ifPresent(ccfg::withMaxErrorRetry);
        cfg.getOptional("client_user_agent_prefix").ifPresent(ccfg::withUserAgentPrefix);
        cfg.getOptional("client_consecutive_retries_before_throttling").map(Integer::parseInt)
            .ifPresent(ccfg::withMaxConsecutiveRetriesBeforeThrottling);
        cfg.getOptional("client_gzip").map(Boolean::parseBoolean).ifPresent(ccfg::withGzip);
        cfg.getOptional("client_tcp_keepalive").map(Boolean::parseBoolean).ifPresent(ccfg::withTcpKeepAlive);
        cfg.getOptional("client_disable_socket_proxy").map(Boolean::parseBoolean).ifPresent(ccfg::withDisableSocketProxy);
// ccfg.withHeader();
// ccfg.withProtocol()
// ccfg.withRetryMode();
// ccfg.withRetryPolicy();

        ccfg.withSocketBufferSizeHints(
            cfg.getOptional("client_so_send_size_hint").map(Integer::parseInt).orElse(0),
            cfg.getOptional("client_so_recv_size_hint").map(Integer::parseInt).orElse(0)
        );

        builder.setClientConfiguration(ccfg);

        return builder.build();
    }

    public static NBConfigModel getConfigModel() {
        return ConfigModel.of(KeyspacesSpace.class)
            .add(Param.optional("endpoint"))
            .add(Param.optional("signing_region"))
            .add(Param.optional("region"))

            .add(Param.optional("client_socket_timeout"))
            .add(Param.optional("client_execution_timeout"))
            .add(Param.optional("client_max_connections"))
            .add(Param.optional("client_max_error_retry"))
            .add(Param.optional("client_user_agent_prefix"))
            .add(Param.optional("client_consecutive_retries_before_throttling"))
            .add(Param.optional("client_gzip"))
            .add(Param.optional("client_tcp_keepalive"))
            .add(Param.optional("client_disable_socket_proxy"))
            .add(Param.optional("client_so_send_size_hint"))
            .add(Param.optional("client_so_recv_size_hint"))
            .asReadOnly();
    }

}
