package org.qubership.integration.platform.runtime.catalog.model.exportimport.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.sql.Timestamp;

@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextServiceLegacyDto {
    private String id;
    private String name;
    private String description;
    private String internalServiceName;
    private Timestamp modifiedWhen;
    private String migrations;
}
