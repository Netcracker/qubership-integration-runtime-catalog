package org.qubership.integration.platform.runtime.catalog.cr;

import lombok.Builder;
import lombok.Getter;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.CustomResourceOptions;

@Getter
@Builder
public class ResourceBuildContext {
    private final String buildVersion;
    private CustomResourceOptions options;
}
