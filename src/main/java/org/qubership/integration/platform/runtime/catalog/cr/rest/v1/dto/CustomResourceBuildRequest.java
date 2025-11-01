package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomResourceBuildRequest {
    @NonNull
    private CustomResourceOptions options;

    @Builder.Default
    private List<String> chainIds = Collections.emptyList();
}
