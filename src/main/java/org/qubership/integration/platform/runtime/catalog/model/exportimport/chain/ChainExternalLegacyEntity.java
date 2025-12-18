package org.qubership.integration.platform.runtime.catalog.model.exportimport.chain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.BaseExternalEntity;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.exportimport.remoteimport.ChainCommitRequestAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChainExternalLegacyEntity extends BaseExternalEntity {

    private String businessDescription;
    private String assumptions;
    private String outOfScope;
    private String lastImportHash;

    private List<String> labels;
    @Builder.Default
    private Set<MaskedFieldExternalEntity> maskedFields = new HashSet<>();
    @JsonProperty("default-swimlane-id")
    private String defaultSwimlaneId;
    @JsonProperty("reuse-swimlane-id")
    private String reuseSwimlaneId;
    @Builder.Default
    private List<ChainElementExternalEntity> elements = new ArrayList<>();
    @Builder.Default
    private List<DependencyExternalEntity> dependencies = new ArrayList<>();
    private FolderExternalEntity folder;
    @Builder.Default
    private List<DeploymentExternalEntity> deployments = new ArrayList<>();
    private ChainCommitRequestAction deployAction;
    private Integer fileVersion;
    @JsonIgnore
    private boolean overridden;
    @JsonIgnore
    private String overridesChainId;
    @JsonIgnore
    private String overriddenByChainId;
    private String migrations;
}
