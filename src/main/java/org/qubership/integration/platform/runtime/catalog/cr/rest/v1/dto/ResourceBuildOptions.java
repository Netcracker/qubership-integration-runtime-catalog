package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceBuildOptions {
    @Builder.Default
    private String language = "xml";

    private String name;

    @NonNull
    private String image;
}
