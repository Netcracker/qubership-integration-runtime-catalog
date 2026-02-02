package org.qubership.integration.platform.runtime.catalog.cr.cfg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceDefinition {
    private String id;
    private String name;
    private String language;
    private String location;
}
