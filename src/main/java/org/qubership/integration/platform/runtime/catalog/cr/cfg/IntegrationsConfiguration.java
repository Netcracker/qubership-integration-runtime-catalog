package org.qubership.integration.platform.runtime.catalog.cr.cfg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationsConfiguration {
    List<SourceDefinition> chains;
    List<LibraryDefinition> libraries;
}
