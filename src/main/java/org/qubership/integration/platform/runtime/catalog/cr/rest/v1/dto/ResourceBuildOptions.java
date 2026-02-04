package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceBuildOptions {
    @Builder.Default
    private String language = "xml";

    private String name;

    private String namespace;

    @Builder.Default
    private ContainerOptions container = new ContainerOptions();

    @Builder.Default
    private MonitoringOptions monitoring = new MonitoringOptions();

    @Builder.Default
    private Map<String, String> environment = new HashMap<>();

    @Builder.Default
    private IntegrationsConfigurationOptions integrations = new IntegrationsConfigurationOptions();

    private String serviceAccount;
}
