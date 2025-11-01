package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomResourceOptions {
    @Builder.Default
    private String language = "xml";

    @NonNull
    private String image;
}
