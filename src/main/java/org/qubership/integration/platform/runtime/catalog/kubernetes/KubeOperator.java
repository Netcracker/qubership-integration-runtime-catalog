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

package org.qubership.integration.platform.runtime.catalog.kubernetes;

import com.coreos.monitoring.models.V1ServiceMonitor;
import com.coreos.monitoring.models.V1ServiceMonitorList;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceDeployError;
import org.qubership.integration.platform.runtime.catalog.cr.k8s.CamelKIntegration;
import org.qubership.integration.platform.runtime.catalog.cr.k8s.CamelKIntegrationList;
import org.qubership.integration.platform.runtime.catalog.exception.exceptions.kubernetes.KubeApiException;
import org.qubership.integration.platform.runtime.catalog.model.kubernetes.operator.KubeDeployment;
import org.qubership.integration.platform.runtime.catalog.model.kubernetes.operator.KubePod;
import org.qubership.integration.platform.runtime.catalog.model.kubernetes.operator.PodRunningStatus;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
public class KubeOperator {
    private static final String BUILD_VERSION_LABEL = "app.kubernetes.io/version";
    private static final String DEFAULT_ERR_MESSAGE = "Invalid k8s cluster parameters or API error. ";
    private static final String REGEX_FOR_SEARCH_BLUEGREEN_SERVICE_NAME = ".*-v\\d+$";
    private final CoreV1Api coreApi;
    private final AppsV1Api appsApi;
    private final CustomObjectsApi customObjectsApi;

    private final String namespace;

    public KubeOperator() {
        coreApi = new CoreV1Api();
        appsApi = new AppsV1Api();
        customObjectsApi = new CustomObjectsApi();
        namespace = null;
    }

    public KubeOperator(ApiClient client, String namespace) {
        coreApi = new CoreV1Api();
        coreApi.setApiClient(client);

        appsApi = new AppsV1Api();
        appsApi.setApiClient(client);

        customObjectsApi = new CustomObjectsApi();
        customObjectsApi.setApiClient(client);

        this.namespace = namespace;
    }

    public List<KubeDeployment> getDeploymentsByLabel(String labelKey) {
        return getDeploymentsByLabel(labelKey, null);
    }

    public List<KubeDeployment> getDeploymentsByLabel(String labelKey, String labelValue) throws KubeApiException {
        String labelSelector = labelKey + (isNull(labelValue) ? "" : (" = " + labelValue));
        try {
            V1DeploymentList list = appsApi.listNamespacedDeployment(namespace).labelSelector(labelSelector).execute();

            return list.getItems().stream()
                    .map(item -> KubeDeployment.builder()
                            .id(Objects.requireNonNull(item.getMetadata().getUid()))
                            .name(Objects.requireNonNull(item.getMetadata()).getName())
                            .labels(Objects.requireNonNull(item.getMetadata()).getLabels())
                            .namespace(namespace)
                            .replicas(Objects.requireNonNull(item.getSpec().getReplicas()))
                            .version(Objects.requireNonNull(item.getMetadata().getLabels()).get(BUILD_VERSION_LABEL))
                            .build())
                    .collect(Collectors.toList());

        } catch (ApiException e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getResponseBody());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getMessage());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getMessage(), e);
        }
    }

    public List<KubePod> getPodsByLabel(String labelKey, String labelValue) throws KubeApiException {
        try {
            V1PodList list = coreApi.listNamespacedPod(namespace).labelSelector(labelKey + "=" + labelValue).execute();

            return list.getItems().stream()
                    .map(item -> {
                        boolean ready = false;
                        if (item.getStatus() != null
                                && item.getStatus().getContainerStatuses() != null
                                && !item.getStatus().getContainerStatuses().isEmpty()) {
                            ready = item.getStatus().getContainerStatuses().get(0).getReady();
                        }

                        return KubePod.builder()
                                .name(Objects.requireNonNull(item.getMetadata().getName()))
                                .runningStatus(PodRunningStatus.get(Objects.requireNonNull(item.getStatus()).getPhase()))
                                .ready(ready)
                                .ip(item.getStatus().getPodIP())
                                .namespace(namespace)
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (ApiException e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getResponseBody());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getMessage());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getMessage(), e);
        }
    }

    public List<KubeService> getServices() {
        try {
            V1ServiceList list = coreApi.listNamespacedService(namespace).execute();

            return list.getItems().stream()
                    .filter(item -> !(Objects.requireNonNull(Objects.requireNonNull(item.getMetadata()).getName()).matches(REGEX_FOR_SEARCH_BLUEGREEN_SERVICE_NAME)))
                    .map(item -> KubeService.builder()
                            .id(Objects.requireNonNull(Objects.requireNonNull(item.getMetadata()).getUid()))
                            .name(Objects.requireNonNull(item.getMetadata().getName()))
                            .namespace(namespace)
                            .ports(
                                    Objects.requireNonNull(Objects.requireNonNull(item.getSpec()).getPorts()).stream()
                                            .map(V1ServicePort::getPort).collect(Collectors.toList()
                                            )).build())
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getResponseBody());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error(DEFAULT_ERR_MESSAGE + e.getMessage());
            throw new KubeApiException(DEFAULT_ERR_MESSAGE + e.getMessage(), e);
        }
    }

    public void createOrUpdateResource(Object resource) throws ApiException {
        if (resource instanceof V1ConfigMap cm) {
            createOrUpdateConfigMap(cm);
        } else if (resource instanceof V1Service service) {
            createOrUpdateService(service);
        } else if (resource instanceof CamelKIntegration integration) {
            createOrUpdateCustomResource("camel.apache.org", "v1", "integrations",
                    integration, new TypeToken<CamelKIntegrationList>(){}.getType());
        } else if (resource instanceof V1ServiceMonitor serviceMonitor) {
            createOrUpdateCustomResource("monitoring.coreos.com", "v1", "servicemonitors",
                    serviceMonitor, new TypeToken<V1ServiceMonitorList>(){}.getType());
        } else {
            throw new CustomResourceDeployError("Unsupported resource type: " + resource);
        }
    }

    private void createOrUpdateConfigMap(V1ConfigMap cm) throws ApiException {
        V1ConfigMapList configMapList = coreApi.listNamespacedConfigMap(namespace).execute();
        if (listContains(configMapList, cm)) {
            coreApi.replaceNamespacedConfigMap(getName(cm).orElse(""), namespace, cm).execute();
        } else {
            coreApi.createNamespacedConfigMap(namespace, cm).execute();
        }
    }

    private void createOrUpdateService(V1Service service) throws ApiException {
        V1ServiceList serviceList = coreApi.listNamespacedService(namespace).execute();
        if (listContains(serviceList, service)) {
            coreApi.replaceNamespacedService(getName(service).orElse(""), namespace, service).execute();
        } else {
            coreApi.createNamespacedService(namespace, service).execute();
        }
    }

    private void createOrUpdateCamelKIntegration(CamelKIntegration integration) throws ApiException {
        Object rawObj = customObjectsApi.listNamespacedCustomObject("camel.apache.org", "v1", namespace, "integrations").execute();
        CamelKIntegrationList integrationList = JSON.deserialize(JSON.serialize(rawObj), new TypeToken<CamelKIntegrationList>(){}.getType());
        Optional<String> name = getName(integration);
        Optional<V1ObjectMeta> existingItemMetadata = integrationList.getItems()
                .stream()
                .filter(item -> getName(item).equals(name))
                .map(KubernetesObject::getMetadata)
                .findAny();
        boolean alreadyExists = existingItemMetadata.isPresent();
        if (alreadyExists) {
            String resourceVersion = existingItemMetadata.map(V1ObjectMeta::getResourceVersion).orElse(null);
            integration.getMetadata().setResourceVersion(resourceVersion);
            customObjectsApi.replaceNamespacedCustomObject("camel.apache.org", "v1", namespace, "integrations", name.orElse(""), integration).execute();
        } else {
            customObjectsApi.createNamespacedCustomObject("camel.apache.org", "v1", namespace, "integrations", integration).execute();
        }
    }

    private <T extends KubernetesObject> void createOrUpdateCustomResource(String group, String version, String plural, T obj, Type listType) throws ApiException {
        Object rawListObj = customObjectsApi.listNamespacedCustomObject(group, version, namespace, plural).execute();
        KubernetesListObject listObject = JSON.deserialize(JSON.serialize(rawListObj), listType);
        Optional<String> name = getName(obj);
        Optional<V1ObjectMeta> existingItemMetadata = listObject.getItems()
                .stream()
                .filter(item -> getName(item).equals(name))
                .map(KubernetesObject::getMetadata)
                .findAny();
        boolean alreadyExists = existingItemMetadata.isPresent();
        if (alreadyExists) {
            String resourceVersion = existingItemMetadata.map(V1ObjectMeta::getResourceVersion).orElse(null);
            obj.getMetadata().setResourceVersion(resourceVersion);
            customObjectsApi.replaceNamespacedCustomObject(group, version, namespace, plural, name.orElse(""), obj).execute();
        } else {
            customObjectsApi.createNamespacedCustomObject(group, version, namespace, plural, obj).execute();
        }
    }

    private boolean listContains(KubernetesListObject objectList, KubernetesObject object) {
        Optional<String> name = getName(object);
        return objectList.getItems().stream()
                .anyMatch(m -> getName(m).equals(name));
    }

    private Optional<String> getName(KubernetesObject obj) {
        return Optional.ofNullable(obj.getMetadata()).map(V1ObjectMeta::getName);
    }
}
