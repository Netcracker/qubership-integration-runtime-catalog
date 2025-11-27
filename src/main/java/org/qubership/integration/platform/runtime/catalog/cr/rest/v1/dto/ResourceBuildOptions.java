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
    private String image;

    @Builder.Default
    private Map<String, String> environment = new HashMap<>();
}
