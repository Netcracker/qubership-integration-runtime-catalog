package org.qubership.integration.platform.runtime.catalog.model.exportimport.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.qubership.integration.platform.runtime.catalog.model.system.SystemModelSource;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.User;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.Operation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemModelLegacyDto {
    private String id;
    private String name;
    private String description;
    private Timestamp createdWhen;
    private Timestamp modifiedWhen;
    private User createdBy;
    private User modifiedBy;
    private boolean deprecated;
    private String version;
    private SystemModelSource source;

    @Builder.Default
    @JsonIgnoreProperties({"createdWhen", "modifiedWhen", "createdBy", "modifiedBy"})
    private List<Operation> operations = new ArrayList<>();

    @JsonProperty("parentId")
    private String parentId;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Builder.Default
    private List<SpecificationSourceDto> specificationSources = new ArrayList<>();

    @Builder.Default
    private List<String> labels = new ArrayList<>();
}
