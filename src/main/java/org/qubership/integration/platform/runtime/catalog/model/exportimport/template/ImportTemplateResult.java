package org.qubership.integration.platform.runtime.catalog.model.exportimport.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Result object of template import")
public class ImportTemplateResult {
    @Schema(description = "Id")
    private String id;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Archive name that template were imported from")
    private String archiveName;

    @Schema(description = "Template import status")
    private ImportTemplateStatus status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Warning or error message (if any)")
    private String message;
}
