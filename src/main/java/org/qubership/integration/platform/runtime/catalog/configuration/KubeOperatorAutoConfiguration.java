/*
 * Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.integration.platform.runtime.catalog.configuration;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.TokenFileAuthentication;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.kubernetes.KubeOperator;
import org.qubership.integration.platform.runtime.catalog.kubernetes.secret.DefaultKubeSecretOperator;
import org.qubership.integration.platform.runtime.catalog.kubernetes.secret.KubeSecretOperator;
import org.qubership.integration.platform.runtime.catalog.kubernetes.secret.LocalDevKubeSecretOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;


@Slf4j
@AutoConfiguration
public class KubeOperatorAutoConfiguration {
    private final String uri;
    @Getter
    private final String namespace;
    private final String token;
    private final String cert;

    @Autowired
    public KubeOperatorAutoConfiguration(
        @Value("${kubernetes.cluster.uri}") String uri,
        @Value("${kubernetes.cluster.namespace}") String namespace,
        @Value("${kubernetes.service-account.token}") String token,
        @Value("${kubernetes.service-account.cert}") String cert,
        @Value("${kubernetes.cluster.token:#{null}}") Optional<String> devToken
    ) {
        this.uri = uri;
        this.namespace = namespace;
        this.token = devToken.orElse(token);
        this.cert = cert;
    }

    /**
     * Inside cluster ApiClient configuration
     * Uses the service account created during deployment for the catalog pod
     */
    @Bean
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "false", matchIfMissing = true)
    public KubeOperator kubeOperator() {
        try {
            log.info("Creating KubernetesOperator bean in PROD mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setCertificateAuthority(Files.readAllBytes(Paths.get(cert)))
                    .setAuthentication(new TokenFileAuthentication(token))
                    .build();

            return new KubeOperator(client, namespace);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new KubeOperator();
        }
    }

    /**
     * Outside cluster ApiClient configuration
     * Uses the cluster account token
     */
    @Bean
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "true")
    public KubeOperator kubeOperatorDev() {
        try {
            log.info("Creating KubernetesOperator bean in DEV mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setAuthentication(new AccessTokenAuthentication(token))
                    .build();

            return new KubeOperator(client, namespace);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new KubeOperator();
        }
    }

    /**
     * Inside cluster ApiClient configuration
     * Uses the service account created during deployment for the microservice
     */
    @Bean
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "false", matchIfMissing = true)
    public KubeSecretOperator kubeSecretOperator() {
        try {
            log.info("Creating KubernetesSecretOperator bean in PROD mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setCertificateAuthority(Files.readAllBytes(Paths.get(cert)))
                    .setAuthentication(new TokenFileAuthentication(token))
                    .build();

            return new DefaultKubeSecretOperator(client, namespace);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new DefaultKubeSecretOperator();
        }
    }

    /**
     * Outside cluster ApiClient configuration
     * Uses the cluster account token
     */
    @Bean
    @ConditionalOnExpression("${kubernetes.devmode:false} and !${kubernetes.localdev:false}")
    public KubeSecretOperator kubeSecretOperatorDev() {
        try {
            log.info("Creating KubernetesSecretOperator bean in DEV mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setAuthentication(new AccessTokenAuthentication(token))
                    .build();

            return new DefaultKubeSecretOperator(client, namespace);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new DefaultKubeSecretOperator();
        }
    }


    @Bean
    @ConditionalOnExpression("${kubernetes.devmode:false} and ${kubernetes.localdev:false}")
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "true")
    public KubeSecretOperator kubeOperatorLocalDev() {
        log.info("Creating KubernetesSecretOperator for local development mode");
        return new LocalDevKubeSecretOperator();
    }

}
