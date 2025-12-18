package org.qubership.integration.platform.runtime.catalog.service.exportimport.mapper;

import org.qubership.integration.platform.runtime.catalog.model.exportimport.chain.ChainExternalContentEntity;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.chain.ChainExternalEntity;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.chain.ChainExternalLegacyEntity;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.system.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ExternalEntityLegacyMapper {
    private static final String LEGACY_MIGRATION_VERSION = "101";

    public ChainExternalLegacyEntity mapChainToLegacyEntity(ChainExternalEntity chainExternalEntity) {
        ChainExternalContentEntity content = chainExternalEntity.getContent();

        return ChainExternalLegacyEntity.builder()
                .id(chainExternalEntity.getId())
                .name(chainExternalEntity.getName())

                .businessDescription(content.getBusinessDescription())
                .assumptions(content.getAssumptions())
                .outOfScope(content.getOutOfScope())
                .lastImportHash(content.getLastImportHash())
                .labels(content.getLabels())
                .maskedFields(content.getMaskedFields())
                .defaultSwimlaneId(content.getDefaultSwimlaneId())
                .reuseSwimlaneId(content.getReuseSwimlaneId())
                .elements(content.getElements())
                .dependencies(content.getDependencies())
                .folder(content.getFolder())
                .deployments(content.getDeployments())
                .deployAction(content.getDeployAction())
                .fileVersion(content.getFileVersion())
                .overridden(content.isOverridden())
                .overridesChainId(content.getOverridesChainId())
                .overriddenByChainId(content.getOverriddenByChainId())

                .migrations(remove101Migration(content.getMigrations()))
                .build();
    }

    public IntegrationSystemLegacyDto mapIntegrationSystemToLegacyDto(IntegrationSystemDto integrationSystemDto) {
        IntegrationSystemContentDto content = integrationSystemDto.getContent();

        return IntegrationSystemLegacyDto.builder()
                .id(integrationSystemDto.getId())
                .name(integrationSystemDto.getName())

                .integrationSystemType(content.getIntegrationSystemType())
                .labels(content.getLabels())
                .activeEnvironmentId(content.getActiveEnvironmentId())
                .description(content.getDescription())
                .environments(content.getEnvironments())
                .internalServiceName(content.getInternalServiceName())
                .protocol(content.getProtocol())
                .specificationGroups(content.getSpecificationGroups())
                .createdBy(content.getCreatedBy())
                .createdWhen(content.getCreatedWhen())
                .modifiedBy(content.getModifiedBy())
                .modifiedWhen(content.getModifiedWhen())

                .migrations(remove101Migration(content.getMigrations()))
                .build();

    }

    public SpecificationGroupLegacyDto mapSpecificationGroupToLegacyDto(SpecificationGroupDto specificationGroupDto) {
        SpecificationGroupContentDto content = specificationGroupDto.getContent();

        return SpecificationGroupLegacyDto.builder()
                .id(specificationGroupDto.getId())
                .name(specificationGroupDto.getName())

                .description(content.getDescription())
                .url(content.getUrl())
                .synchronization(content.isSynchronization())
                .parentId(content.getParentId())
                .labels(content.getLabels())
                .createdBy(content.getCreatedBy())
                .createdWhen(content.getCreatedWhen())
                .modifiedBy(content.getModifiedBy())
                .modifiedWhen(content.getModifiedWhen())
                .build();
    }

    public SystemModelLegacyDto mapSystemModelToLegacyDto(SystemModelDto systemModelDto) {
        SystemModelContentDto content = systemModelDto.getContent();

        return SystemModelLegacyDto.builder()
                .id(systemModelDto.getId())
                .name(systemModelDto.getName())

                .description(content.getDescription())
                .deprecated(content.isDeprecated())
                .version(content.getVersion())
                .source(content.getSource())
                .operations(content.getOperations())
                .parentId(content.getParentId())
                .specificationSources(content.getSpecificationSources())
                .labels(content.getLabels())
                .createdBy(content.getCreatedBy())
                .createdWhen(content.getCreatedWhen())
                .modifiedBy(content.getModifiedBy())
                .modifiedWhen(content.getModifiedWhen())
                .build();
    }

    public ContextServiceLegacyDto mapContextServiceToLegacyDto(ContextServiceDto contextServiceDto) {
        ContextServiceContentDto content = contextServiceDto.getContent();

        return ContextServiceLegacyDto.builder()
                .id(contextServiceDto.getId())
                .name(contextServiceDto.getName())

                .description(content.getDescription())
                .internalServiceName(content.getInternalServiceName())
                .modifiedWhen(content.getModifiedWhen())

                .migrations(remove101Migration(content.getMigrations()))
                .build();
    }

    private String remove101Migration(String migrations) {
        if (migrations == null || migrations.isBlank()) {
            return migrations;
        }

        String result = Arrays.stream(migrations.split(","))
                .map(String::trim)
                .filter(v -> !v.equals(LEGACY_MIGRATION_VERSION))
                .collect(Collectors.joining(", "));

        return result.isBlank() ? null : result;
    }
}
