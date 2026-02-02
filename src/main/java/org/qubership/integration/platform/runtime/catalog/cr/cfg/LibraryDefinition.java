package org.qubership.integration.platform.runtime.catalog.cr.cfg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryDefinition {
    private String specificationId;
    private String location;
}
