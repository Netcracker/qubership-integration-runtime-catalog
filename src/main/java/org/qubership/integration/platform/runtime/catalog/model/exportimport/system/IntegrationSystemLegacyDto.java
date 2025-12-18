package org.qubership.integration.platform.runtime.catalog.model.exportimport.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.BaseExternalEntity;
import org.qubership.integration.platform.runtime.catalog.model.system.IntegrationSystemType;
import org.qubership.integration.platform.runtime.catalog.model.system.OperationProtocol;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.User;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.Environment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationSystemLegacyDto extends BaseExternalEntity {

    private Timestamp createdWhen;
    private User createdBy;
    private User modifiedBy;
    private String activeEnvironmentId;
    private IntegrationSystemType integrationSystemType;
    private String internalServiceName;
    private OperationProtocol protocol;

    @Builder.Default
    @JsonIgnoreProperties({"createdWhen", "modifiedWhen", "createdBy", "modifiedBy"})
    private List<Environment> environments = new ArrayList<>();

    @Builder.Default
    private List<SpecificationGroupDto> specificationGroups = new ArrayList<>();

    @Builder.Default
    private List<String> labels = new ArrayList<>();

    private String migrations;
}
