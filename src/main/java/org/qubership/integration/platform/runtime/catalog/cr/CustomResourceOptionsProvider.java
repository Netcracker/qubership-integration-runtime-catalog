package org.qubership.integration.platform.runtime.catalog.cr;

import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class CustomResourceOptionsProvider {
    @Value("${qip.cr.build.container.image}")
    private String containerImage;

    @Value("${qip.cr.build.container.image-pool-policy:IfNotPresent}")
    private ImagePoolPolicy imagePoolPolicy;

    @Value("${qip.cr.build.monitoring.enabled:false}")
    private boolean monitoringEnabled;

    @Value("${qip.cr.build.monitoring.interval:30s}")
    private String interval;

    @Value("${qip.cr.build.service-account:default}")
    private String serviceAccount;

    public ResourceBuildOptions getOptions(ResourceDeployRequest request) {
        return ResourceBuildOptions.builder()
                .name(request.getName())
                .container(ContainerOptions.builder()
                        .image(containerImage)
                        .imagePoolPolicy(imagePoolPolicy)
                        .build())
                .monitoring(MonitoringOptions.builder()
                        .enabled(monitoringEnabled)
                        .interval(interval)
                        .build())
                .integrations(IntegrationsConfigurationOptions.builder()
                        .camelKSourcesUtilized(false)
                        .build())
                .environment(Map.of(
                        // FIXME
                        "CONSUL_URL", "configmap:qip-env/CONSUL_URL",
                        "CONSUL_ADMIN_TOKEN", "configmap:qip-env/CONSUL_ADMIN_TOKEN",
                        "OPENSEARCH_HOST", "configmap:qip-env/OPENSEARCH_HOST",
                        "OPENSEARCH_PORT", "configmap:qip-env/OPENSEARCH_PORT",
                        "POSTGRES_URL", "configmap:qip-engine-env/POSTGRES_URL",
                        "POSTGRES_USER", "secret:qip-postgres-auth/username",
                        "POSTGRES_PASSWORD", "secret:qip-postgres-auth/password",
                        "MONITORING_ENABLED", Boolean.valueOf(monitoringEnabled).toString()
                ))
                .serviceAccount(serviceAccount)
                .build();
    }
}
