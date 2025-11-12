package org.qubership.integration.platform.runtime.catalog.cr.sources;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SourceBuilderContext {
    private String buildVersion;
}
