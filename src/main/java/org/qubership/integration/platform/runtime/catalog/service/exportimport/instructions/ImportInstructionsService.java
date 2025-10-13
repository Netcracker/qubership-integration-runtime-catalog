/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.integration.platform.runtime.catalog.service.exportimport.instructions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.integration.platform.runtime.catalog.consul.ConsulService;
import org.qubership.integration.platform.runtime.catalog.exception.exceptions.ImportInstructionsExternalException;
import org.qubership.integration.platform.runtime.catalog.exception.exceptions.ImportInstructionsInternalException;
import org.qubership.integration.platform.runtime.catalog.exception.exceptions.ImportInstructionsValidationException;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions.*;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions.variables.CommonVariablesImportInstructionsConfig;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions.variables.PerformInstructionsResult;
import org.qubership.integration.platform.runtime.catalog.model.filter.FilterCondition;
import org.qubership.integration.platform.runtime.catalog.model.filter.FilterFeature;
import org.qubership.integration.platform.runtime.catalog.model.mapper.mapping.exportimport.instructions.CommonVariablesInstructionsMapper;
import org.qubership.integration.platform.runtime.catalog.model.mapper.mapping.exportimport.instructions.GeneralInstructionsMapper;
import org.qubership.integration.platform.runtime.catalog.model.mapper.mapping.exportimport.instructions.ServiceInstructionsMapper;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.actionlog.ActionLog;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.actionlog.EntityType;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.actionlog.LogOperation;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.instructions.ImportInstruction;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.repository.instructions.ImportInstructionsRepository;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.FilterRequestDTO;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.exportimport.instructions.DeleteInstructionsRequest;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.exportimport.remoteimport.ChainCommitRequestAction;
import org.qubership.integration.platform.runtime.catalog.service.*;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.CommonVariablesImportService;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.ExportImportUtils;
import org.qubership.integration.platform.runtime.catalog.service.filter.ImportInstructionFilterSpecificationBuilder;
import org.qubership.integration.platform.runtime.catalog.validation.EntityValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ImportInstructionsService {

    public static final String UNIQUE_OVERRIDE_DB_CONSTRAINT_NAME = "import_instructions_unique_override_idx";
    public static final String OVERRIDE_ACTION_DB_CONSTRAINT_NAME = "overridden_by_id_only_for_override";
    private static final String FAILED_TO_READ_CONFIG_FILE_MESSAGE = "Failed to read import instructions config file: ";

    @Getter
    private final String instructionsFileName;
    private final YAMLMapper yamlMapper;
    private final ImportInstructionsRepository importInstructionsRepository;
    private final GeneralInstructionsMapper generalInstructionsMapper;
    private final ServiceInstructionsMapper serviceInstructionsMapper;
    private final ChainService chainService;
    private final DeploymentService deploymentService;
    private final SystemService systemService;
    private final SpecificationGroupService specificationGroupService;
    private final SystemModelService systemModelService;
    private final CommonVariablesImportService commonVariablesImportService;
    private final CommonVariablesInstructionsMapper commonVariablesInstructionsMapper;
    private final EntityValidator entityValidator;
    private final ActionsLogService actionsLogService;
    private final ImportInstructionFilterSpecificationBuilder importInstructionFilterSpecificationBuilder;
    private final ConsulService consulService;

    @Autowired
    public ImportInstructionsService(
            @Value("${qip.import.instructions-filename:qip-import-instructions}") String instructionsFileName,
            ImportInstructionsRepository importInstructionsRepository,
            GeneralInstructionsMapper generalInstructionsMapper,
            ServiceInstructionsMapper serviceInstructionsMapper,
            ChainService chainService,
            DeploymentService deploymentService,
            SystemService systemService,
            SpecificationGroupService specificationGroupService,
            SystemModelService systemModelService,
            CommonVariablesImportService commonVariablesImportService,
            CommonVariablesInstructionsMapper commonVariablesInstructionsMapper,
            EntityValidator entityValidator,
            ActionsLogService actionsLogService,
            ImportInstructionFilterSpecificationBuilder importInstructionFilterSpecificationBuilder,
            ConsulService consulService
    ) {
        this.instructionsFileName = instructionsFileName + ".yaml";
        this.yamlMapper = new YAMLMapper().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        this.importInstructionsRepository = importInstructionsRepository;
        this.generalInstructionsMapper = generalInstructionsMapper;
        this.serviceInstructionsMapper = serviceInstructionsMapper;
        this.chainService = chainService;
        this.deploymentService = deploymentService;
        this.systemService = systemService;
        this.specificationGroupService = specificationGroupService;
        this.systemModelService = systemModelService;
        this.commonVariablesImportService = commonVariablesImportService;
        this.commonVariablesInstructionsMapper = commonVariablesInstructionsMapper;
        this.entityValidator = entityValidator;
        this.actionsLogService = actionsLogService;
        this.importInstructionFilterSpecificationBuilder = importInstructionFilterSpecificationBuilder;
        this.consulService = consulService;
    }

    public List<ImportInstruction> getAllImportInstructions() {
        List<ImportInstruction> importInstructions = new ArrayList<>(getImportInstructions());

        ImportInstructionsDTO variablesInstructionsDTO = commonVariablesImportService.getCommonVariablesImportInstructions();
        if (variablesInstructionsDTO != null) {
            importInstructions.addAll(commonVariablesInstructionsMapper.asEntities(variablesInstructionsDTO));
        }
        return importInstructions;
    }

    public List<ImportInstruction> getChainImportInstructions(Collection<ImportInstructionAction> actions) {
        return importInstructionsRepository.findByEntityTypeAndActionIn(ImportEntityType.CHAIN, actions);
    }

    public List<ImportInstruction> getServiceImportInstructions(Collection<ImportInstructionAction> actions) {
        return importInstructionsRepository.findByEntityTypeAndActionIn(ImportEntityType.SERVICE, actions);
    }

    public ImportInstructionsConfig getServiceImportInstructionsConfig(Collection<ImportInstructionAction> actions) {
        return serviceInstructionsMapper.asConfig(getServiceImportInstructions(actions));
    }

    public List<ImportInstruction> getImportInstructions() {
        return importInstructionsRepository.findAll();
    }

    public List<ImportInstruction> getImportInstructions(Collection<FilterRequestDTO> filterRequestDTOS) {
        Specification<ImportInstruction> specification = importInstructionFilterSpecificationBuilder
                .buildFilter(filterRequestDTOS);
        return importInstructionsRepository.findAll(specification);
    }

    public List<ImportInstruction> getImportInstructionsForPreview(File importInstructionsConfigFile) {
        GeneralImportInstructionsConfig importInstructionsConfig;
        try {
            importInstructionsConfig = parseImportInstructionsConfig(
                    importInstructionsConfigFile.getName(),
                    FileUtils.readFileToByteArray(importInstructionsConfigFile)
            );
        } catch (IOException e) {
            log.error(FAILED_TO_READ_CONFIG_FILE_MESSAGE + "{}", importInstructionsConfigFile, e);
            throw new ImportInstructionsInternalException(
                    FAILED_TO_READ_CONFIG_FILE_MESSAGE + importInstructionsConfigFile.getName(), e);
        }

        List<ImportInstruction> importInstructions = new ArrayList<>(
                generalInstructionsMapper.asEntitiesIncludingDeletes(importInstructionsConfig)
        );
        for (ImportInstruction storedImportInstruction : getAllImportInstructions()) {
            if (ImportEntityType.COMMON_VARIABLE == storedImportInstruction.getEntityType()) {
                boolean noInstructionInConfig = importInstructions.stream()
                        .noneMatch(importInstruction -> importInstruction.equals(storedImportInstruction)
                                && importInstruction.getEntityType() == ImportEntityType.COMMON_VARIABLE);
                if (noInstructionInConfig) {
                    importInstructions.add(storedImportInstruction);
                }
                continue;
            }
            if (!importInstructions.contains(storedImportInstruction)) {
                importInstructions.add(storedImportInstruction);
            }
        }

        return importInstructions;
    }

    public List<ImportInstruction> saveImportInstructions(GeneralImportInstructionsConfig generalImportInstructionsConfig) {
        return importInstructionsRepository.saveAll(generalInstructionsMapper.asEntities(generalImportInstructionsConfig));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ImportInstructionResult> uploadImportInstructionsConfig(MultipartFile file, Set<String> labels) {
        try {
            return uploadImportInstructionsConfig(file.getOriginalFilename(), file.getBytes(), labels);
        } catch (IOException e) {
            log.error(FAILED_TO_READ_CONFIG_FILE_MESSAGE + "{}", file.getOriginalFilename(), e);
            throw new ImportInstructionsInternalException(FAILED_TO_READ_CONFIG_FILE_MESSAGE + file.getOriginalFilename(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ImportInstructionResult> uploadImportInstructionsConfig(File file, Set<String> labels) {
        try {
            return uploadImportInstructionsConfig(file.getName(), FileUtils.readFileToByteArray(file), labels);
        } catch (IOException e) {
            log.error(FAILED_TO_READ_CONFIG_FILE_MESSAGE + "{}", file.getName(), e);
            throw new ImportInstructionsInternalException(FAILED_TO_READ_CONFIG_FILE_MESSAGE + file.getName(), e);
        }
    }

    private List<ImportInstructionResult> uploadImportInstructionsConfig(
            String fileName,
            byte[] fileContent,
            Set<String> labels
    ) {
        GeneralImportInstructionsConfig importInstructionsConfig = parseImportInstructionsConfig(fileName, fileContent);
        importInstructionsConfig.setLabels(labels);

        entityValidator.validate(importInstructionsConfig);

        logAction(fileName, null, EntityType.IMPORT_INSTRUCTIONS, LogOperation.IMPORT);

        List<ImportInstructionResult> importInstructionResults = new ArrayList<>();
        importInstructionResults.addAll(performChainDeleteInstructions(importInstructionsConfig));
        importInstructionResults.addAll(performSpecificationDeleteInstructions(importInstructionsConfig));
        importInstructionResults.addAll(performSpecificationGroupDeleteInstructions(importInstructionsConfig));
        importInstructionResults.addAll(performServiceDeleteInstructions(importInstructionsConfig));

        List<ImportInstruction> savedImportInstructions = saveImportInstructions(importInstructionsConfig);
        importInstructionResults.addAll(commonVariablesImportService
                .uploadCommonVariablesImportInstructions(fileName, fileContent, labels));

        savedImportInstructions.forEach(importInstruction ->
                logSingleInstructionAction(
                        importInstruction.getId(),
                        importInstruction.getEntityType(),
                        LogOperation.CREATE_OR_UPDATE
                )
        );

        log.info("Import instructions file {} successfully uploaded", fileName);

        return importInstructionResults;
    }

    public Pair<String, byte[]> exportImportInstructions() {
        GeneralImportInstructionsConfig importInstructionsConfig = getAllImportInstructionsConfig();
        try {
            JsonNode instructionsNode = yamlMapper.convertValue(importInstructionsConfig, JsonNode.class);
            StringBuilder importInstructionsYaml = new StringBuilder();
            importInstructionsYaml.append(
                    """
                            # all instructions with delete action won't be stored in DB for ANY section below \
                            and they will be applied immediately during the file upload

                            """);

            Iterator<Map.Entry<String, JsonNode>> nodesIterator = instructionsNode.fields();
            while (nodesIterator.hasNext()) {
                Map.Entry<String, JsonNode> nodeEntry = nodesIterator.next();
                switch (nodeEntry.getKey()) {
                    case "chains" -> importInstructionsYaml.append("# chains section might contain delete, ignore and override actions\n");
                    case "services" -> importInstructionsYaml.append("# services section might contain delete and ignore actions\n");
                    case "specificationGroups" -> importInstructionsYaml.append("# specification groups section might contain only delete action\n");
                    case "specifications" -> importInstructionsYaml.append("# specifications section might contain only delete action\n");
                    case "commonVariables" -> importInstructionsYaml.append("# common variables section might contain delete and ignore actions\n");
                }
                importInstructionsYaml.append(yamlMapper.writeValueAsString(nodeEntry));
            }

            Pair<String, byte[]> exportResult = Pair.of(instructionsFileName, importInstructionsYaml.toString().getBytes());

            logAction(instructionsFileName, null, EntityType.IMPORT_INSTRUCTIONS, LogOperation.EXPORT);

            return exportResult;
        } catch (Exception e) {
            log.error("Error while exporting import instructions", e);
            throw new ImportInstructionsInternalException("Error while exporting import instructions", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChainsIgnoreOverrideResult performChainIgnoreOverrideInstructions(Collection<String> chainsToImport) {
        List<ImportInstructionResult> instructionResults = new ArrayList<>();
        List<ImportInstruction> importInstructions = getChainImportInstructions(
                Set.of(ImportInstructionAction.IGNORE, ImportInstructionAction.OVERRIDE)
        );
        Map<String, ChainCommitRequestAction> filteredChainsToImport = chainsToImport.stream()
                .map(chainId -> Pair.of(chainId, (ChainCommitRequestAction) null))
                .collect(HashMap::new, (map, pair) -> map.put(pair.getKey(), pair.getValue()), HashMap::putAll);
        Map<String, String> overridesMapping = new HashMap<>();
        for (ImportInstruction importInstruction : importInstructions) {
            if (ImportInstructionAction.IGNORE.equals(importInstruction.getAction())) {
                if (filteredChainsToImport.containsKey(importInstruction.getId())) {
                    filteredChainsToImport.remove(importInstruction.getId());
                    instructionResults.add(ImportInstructionResult.builder()
                            .id(importInstruction.getId())
                            .name(importInstruction.getEntityName())
                            .entityType(ImportEntityType.CHAIN)
                            .status(ImportInstructionStatus.IGNORED)
                            .build());
                }
                continue;
            }

            if (chainsToImport.contains(importInstruction.getOverriddenBy())) {
                try {
                    if (
                            chainService.setOverriddenById(importInstruction.getId(), importInstruction.getOverriddenBy())
                                    || chainsToImport.contains(importInstruction.getId())
                    ) {
                        overridesMapping.put(importInstruction.getOverriddenBy(), importInstruction.getId());
                        deploymentService.deleteAllByChainId(importInstruction.getId());

                        log.info("Chain {} overridden by {} as a part of import instructions list",
                                importInstruction.getId(), importInstruction.getOverriddenBy());

                        instructionResults.add(buildOverriddenInstructionResult(importInstruction));
                    }
                } catch (Exception e) {
                    instructionResults.add(handleOverridingException(e, importInstruction));
                    continue;
                }
            }

            if (chainsToImport.contains(importInstruction.getId())) {
                if (!chainsToImport.contains(importInstruction.getOverriddenBy())) {
                    try {
                        if (chainService.setOverridesChainId(importInstruction.getId(), importInstruction.getOverriddenBy())) {
                            overridesMapping.put(importInstruction.getOverriddenBy(), importInstruction.getId());
                            instructionResults.add(buildOverriddenInstructionResult(importInstruction));
                        }
                    } catch (Exception e) {
                        instructionResults.add(handleOverridingException(e, importInstruction));
                        continue;
                    }
                }
                filteredChainsToImport.put(importInstruction.getId(), ChainCommitRequestAction.SNAPSHOT);
            }
        }

        return new ChainsIgnoreOverrideResult(filteredChainsToImport, overridesMapping, instructionResults);
    }

    public IgnoreResult performServiceIgnoreInstructions(Collection<String> servicesToImport, boolean includeResults) {
        Map<String, ImportInstruction> ignoreInstructions = getServiceImportInstructions(Set.of(ImportInstructionAction.IGNORE))
                .stream()
                .collect(Collectors.toMap(ImportInstruction::getId, Function.identity()));

        Set<String> serviceIdsToImport = new HashSet<>();
        List<ImportInstructionResult> importInstructionResults = new ArrayList<>();
        for (String serviceId : servicesToImport) {
            ImportInstruction ignoreInstruction = ignoreInstructions.get(serviceId);
            if (ignoreInstruction == null) {
                serviceIdsToImport.add(serviceId);
                continue;
            }
            if (includeResults) {
                importInstructionResults.add(ImportInstructionResult.builder()
                        .id(ignoreInstruction.getId())
                        .name(ignoreInstruction.getEntityName())
                        .entityType(ImportEntityType.SERVICE)
                        .status(ImportInstructionStatus.IGNORED)
                        .build());
            }
        }
        return new IgnoreResult(serviceIdsToImport, importInstructionResults);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PerformInstructionsResult performVariableIgnoreInstructions(Set<String> variablesToImport) {
        ImportInstructionsConfig instructionsConfig = commonVariablesInstructionsMapper.asConfig(importInstructionsRepository.findAll());

        List<ImportInstructionResult> instructionsExecutionResults = new ArrayList<>();
        Set<String> ignoreSet = instructionsConfig.getIgnore().stream()
                .filter(ignoreInstruction -> {
                    if (variablesToImport.contains(ignoreInstruction)) {
                        instructionsExecutionResults.add(ImportInstructionResult.builder()
                                .id(ignoreInstruction)
                                .entityType(ImportEntityType.COMMON_VARIABLE)
                                .status(ImportInstructionStatus.IGNORED)
                                .build());
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toSet());
        return new PerformInstructionsResult(ignoreSet, instructionsExecutionResults);
    }

    public List<ImportInstruction> searchImportInstructions(String searchCondition) {
        List<FilterRequestDTO> filters = Stream.of(FilterFeature.ID, FilterFeature.OVERRIDDEN_BY)
                .map(feature -> FilterRequestDTO.builder()
                        .feature(feature)
                        .condition(FilterCondition.CONTAINS)
                        .value(searchCondition)
                        .build())
                .collect(Collectors.toList());
        return importInstructionsRepository.findAll(importInstructionFilterSpecificationBuilder.buildSearch(filters));
    }

    @Transactional
    public ImportInstruction addImportInstruction(ImportInstruction importInstruction) {
        validateImportInstruction(importInstruction);

        if (importInstructionsRepository.existsById(importInstruction.getId())) {
            log.error("Instruction for {} already exist", importInstruction.getId());
            throw new ImportInstructionsExternalException("Instruction for " + importInstruction.getId() + " already exist");
        }

        importInstruction = importInstructionsRepository.persistAndReturn(importInstruction);

        logAction(importInstruction.getId(), importInstruction.getEntityType(), EntityType.IMPORT_INSTRUCTION, LogOperation.CREATE);

        return importInstruction;
    }

    @Transactional
    public ImportInstruction updateImportInstruction(ImportInstruction importInstruction) {
        validateImportInstruction(importInstruction);

        ImportInstruction existingImportInstruction = importInstructionsRepository.findById(importInstruction.getId())
                .orElseThrow(() -> {
                    log.error("Instruction with id {} does not exist", importInstruction.getId());
                    return new ImportInstructionsExternalException(
                            "Instruction with id " + importInstruction.getId() + " does not exist"
                    );
                });

        if (importInstruction.getEntityType() != existingImportInstruction.getEntityType()) {
            log.error("Import instruction entity type cannot be updated");
            throw new ImportInstructionsValidationException("Import instruction entity type cannot be updated");
        }

        ImportInstruction resultImportInstruction = importInstructionsRepository
                .mergeAndReturn(existingImportInstruction.patch(importInstruction));

        logAction(resultImportInstruction.getId(), resultImportInstruction.getEntityType(), EntityType.IMPORT_INSTRUCTION, LogOperation.UPDATE);

        return resultImportInstruction;
    }

    @Transactional
    public void deleteImportInstructionsById(DeleteInstructionsRequest deleteInstructionsRequest) {
        Set<String> instructionsToDelete = Stream.of(
                        deleteInstructionsRequest.getChains().stream(),
                        deleteInstructionsRequest.getServices().stream()
                )
                .flatMap(Function.identity())
                .collect(Collectors.toSet());

        List<ImportInstruction> importInstructions = importInstructionsRepository.findAllById(instructionsToDelete);
        importInstructionsRepository.deleteAll(importInstructions);

        importInstructions.forEach(importInstruction ->
                logAction(importInstruction.getId(), importInstruction.getEntityType(), EntityType.IMPORT_INSTRUCTIONS, LogOperation.DELETE)
        );
    }

    private GeneralImportInstructionsConfig getAllImportInstructionsConfig() {
        GeneralImportInstructionsConfig importInstructionsConfig = generalInstructionsMapper
                .asConfig(importInstructionsRepository.findAll());
        importInstructionsConfig.setCommonVariables(commonVariablesImportService.getCommonVariablesImportInstructionsConfig());
        return importInstructionsConfig;
    }

    private GeneralImportInstructionsConfig parseImportInstructionsConfig(String fileName, byte[] fileContent) {
        if (!ExportImportUtils.isYamlFile(fileName)) {
            log.error("File {} must have yaml/yml extension", fileName);
            throw new ImportInstructionsExternalException("File " + fileName + " must have yaml/yml extension");
        }

        try {
            return yamlMapper.readValue(fileContent, GeneralImportInstructionsConfig.class);
        } catch (IOException e) {
            log.error("Unable to parse import instructions config file: {}", fileName, e);
            throw new ImportInstructionsExternalException("Unable to parse import instructions config file: " + fileName, e);
        }
    }

    private List<ImportInstructionResult> performChainDeleteInstructions(GeneralImportInstructionsConfig instructionsConfig) {
        if (
                Optional.ofNullable(instructionsConfig.getChains())
                        .map(ChainImportInstructionsConfig::getDelete)
                        .filter(deleteInstructions -> !deleteInstructions.isEmpty())
                        .isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<ImportInstructionResult> results = new ArrayList<>();
        for (String chainId : instructionsConfig.getChains().getDelete()) {
            try {
                chainService.deleteByIdIfExists(chainId).ifPresentOrElse(
                        chain -> {
                            log.info("Chain {} deleted as a part of import instructions list", chain.getId());

                            results.add(ImportInstructionResult.builder()
                                    .id(chain.getId())
                                    .name(chain.getName())
                                    .entityType(ImportEntityType.CHAIN)
                                    .status(ImportInstructionStatus.DELETED)
                                    .build());
                        },
                        () -> results.add(ImportInstructionResult.builder()
                                .id(chainId)
                                .entityType(ImportEntityType.CHAIN)
                                .status(ImportInstructionStatus.NO_ACTION)
                                .build())
                );
            } catch (Exception e) {
                log.warn("Failed to delete chain {} as a part of import instructions list", chainId, e);
                results.add(ImportInstructionResult.builder()
                        .id(chainId)
                        .entityType(ImportEntityType.CHAIN)
                        .status(ImportInstructionStatus.ERROR_ON_DELETE)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return results;
    }

    private List<ImportInstructionResult> performServiceDeleteInstructions(GeneralImportInstructionsConfig instructionsConfig) {
        if (
                Optional.ofNullable(instructionsConfig.getServices())
                        .map(ImportInstructionsConfig::getDelete)
                        .filter(deleteInstructions -> !deleteInstructions.isEmpty())
                        .isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<ImportInstructionResult> results = new ArrayList<>();
        for (String serviceId : instructionsConfig.getServices().getDelete()) {
            try {
                systemService.deleteByIdAndReturnService(serviceId).ifPresentOrElse(
                        integrationSystem -> {
                            log.info("Service {} deleted as a part of import instructions list", integrationSystem.getId());

                            results.add(ImportInstructionResult.builder()
                                    .id(integrationSystem.getId())
                                    .name(integrationSystem.getName())
                                    .entityType(ImportEntityType.SERVICE)
                                    .status(ImportInstructionStatus.DELETED)
                                    .build());
                        },
                        () -> results.add(ImportInstructionResult.builder()
                                .id(serviceId)
                                .entityType(ImportEntityType.SERVICE)
                                .status(ImportInstructionStatus.NO_ACTION)
                                .build())
                );
            } catch (Exception e) {
                log.warn("Failed to delete service {} as a part of import instructions list", serviceId, e);
                results.add(ImportInstructionResult.builder()
                        .id(serviceId)
                        .entityType(ImportEntityType.SERVICE)
                        .status(ImportInstructionStatus.ERROR_ON_DELETE)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return results;
    }

    private List<ImportInstructionResult> performSpecificationGroupDeleteInstructions(GeneralImportInstructionsConfig instructionsConfig) {
        if (
                Optional.ofNullable(instructionsConfig.getSpecificationGroups())
                        .map(ImportInstructionsConfig::getDelete)
                        .filter(deleteInstructions -> !deleteInstructions.isEmpty())
                        .isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<ImportInstructionResult> results = new ArrayList<>();
        for (String specificationGroupId : instructionsConfig.getSpecificationGroups().getDelete()) {
            try {
                specificationGroupService.deleteByIdExists(specificationGroupId).ifPresentOrElse(
                        specificationGroup -> {
                            log.info("Specification Group {} deleted as a part of import instructions list", specificationGroup.getId());

                            results.add(ImportInstructionResult.builder()
                                    .id(specificationGroup.getId())
                                    .name(specificationGroup.getName())
                                    .entityType(ImportEntityType.SPECIFICATION_GROUP)
                                    .status(ImportInstructionStatus.DELETED)
                                    .build());
                        },
                        () -> results.add(ImportInstructionResult.builder()
                                .id(specificationGroupId)
                                .entityType(ImportEntityType.SPECIFICATION_GROUP)
                                .status(ImportInstructionStatus.NO_ACTION)
                                .build())
                );
            } catch (Exception e) {
                log.warn("Failed to delete specification group {} as a part of import instructions list", specificationGroupId, e);
                results.add(ImportInstructionResult.builder()
                        .id(specificationGroupId)
                        .entityType(ImportEntityType.SPECIFICATION_GROUP)
                        .status(ImportInstructionStatus.ERROR_ON_DELETE)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return results;
    }

    private List<ImportInstructionResult> performSpecificationDeleteInstructions(GeneralImportInstructionsConfig instructionsConfig) {
        if (
                Optional.ofNullable(instructionsConfig.getSpecifications())
                        .map(ImportInstructionsConfig::getDelete)
                        .filter(deleteInstructions -> !deleteInstructions.isEmpty())
                        .isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<ImportInstructionResult> results = new ArrayList<>();
        for (String specificationId : instructionsConfig.getSpecifications().getDelete()) {
            try {
                systemModelService.deleteSystemModelByIdIfExists(specificationId).ifPresentOrElse(
                        specification -> {
                            log.info("Specification {} deleted as a part of import instructions list", specification.getId());

                            results.add(ImportInstructionResult.builder()
                                    .id(specification.getId())
                                    .name(specification.getName())
                                    .entityType(ImportEntityType.SPECIFICATION)
                                    .status(ImportInstructionStatus.DELETED)
                                    .build());
                        },
                        () -> results.add(ImportInstructionResult.builder()
                                .id(specificationId)
                                .entityType(ImportEntityType.SPECIFICATION)
                                .status(ImportInstructionStatus.NO_ACTION)
                                .build())
                );
            } catch (Exception e) {
                log.warn("Failed to delete specification {} as a part of import instructions list", specificationId, e);
                results.add(ImportInstructionResult.builder()
                        .id(specificationId)
                        .entityType(ImportEntityType.SPECIFICATION)
                        .status(ImportInstructionStatus.ERROR_ON_DELETE)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        return results;
    }

    private List<ImportInstructionResult> performVariableDeleteInstructions(
            CommonVariablesImportInstructionsConfig instructionsConfig
    ) {
        if (
                Optional.ofNullable(instructionsConfig.getCommonVariables())
                        .map(ImportInstructionsConfig::getDelete)
                        .filter(deleteInstructions -> !deleteInstructions.isEmpty())
                        .isEmpty()
        ) {
            return Collections.emptyList();
        }

        Map<String, String> existingVariables = consulService.getAllCommonVariables();
        Set<String> deleteIds = instructionsConfig.getCommonVariables().getDelete();
        List<String> filteredDeleteIds = deleteIds.stream()
                .filter(existingVariables::containsKey)
                .collect(Collectors.toList());
        ImportInstructionStatus executionStatus;
        String errorMessage = null;
        try {
            consulService.deleteCommonVariables(filteredDeleteIds);

            logCommonVariablesDeleteActions(filteredDeleteIds);

            executionStatus = ImportInstructionStatus.DELETED;
        } catch (Exception e) {
            executionStatus = ImportInstructionStatus.ERROR_ON_DELETE;
            errorMessage = e.getMessage();
        }

        List<ImportInstructionResult> results = new ArrayList<>();
        for (String deleteId : deleteIds) {
            ImportInstructionStatus executionStatusForVariable = filteredDeleteIds.contains(deleteId)
                    ? executionStatus
                    : ImportInstructionStatus.NO_ACTION;
            if (executionStatusForVariable == ImportInstructionStatus.DELETED) {
                log.info("Variable {} deleted as a part of import exclusion list", deleteId);
            }
            results.add(ImportInstructionResult.builder()
                    .id(deleteId)
                    .entityType(ImportEntityType.COMMON_VARIABLE)
                    .status(executionStatusForVariable)
                    .errorMessage(errorMessage)
                    .build());
        }

        return results;
    }

    private void logSingleInstructionAction(String entityName, ImportEntityType importEntityType, LogOperation logOperation) {
        logAction(entityName, importEntityType, EntityType.IMPORT_INSTRUCTION, logOperation);
    }

    private void logCommonVariablesDeleteActions(Collection<String> variableNames) {
        variableNames.forEach(variableName -> actionsLogService.logAction(
                ActionLog.builder()
                        .entityName(variableName)
                        .entityType(EntityType.COMMON_VARIABLE)
                        .operation(LogOperation.DELETE)
                        .build()
        ));
    }

    private void logAction(
            String entityName,
            ImportEntityType importEntityType,
            EntityType logEntityType,
            LogOperation logOperation
    ) {
        actionsLogService.logAction(ActionLog.builder()
                .entityName(entityName)
                .parentName(importEntityType != null ? importEntityType.name() : null)
                .entityType(logEntityType)
                .operation(logOperation)
                .build());
    }

    private ImportInstructionResult buildOverriddenInstructionResult(ImportInstruction importInstruction) {
        return ImportInstructionResult.builder()
                .id(importInstruction.getId())
                .name(importInstruction.getEntityName())
                .entityType(ImportEntityType.CHAIN)
                .status(ImportInstructionStatus.OVERRIDDEN)
                .build();
    }

    private ImportInstructionResult handleOverridingException(Exception e, ImportInstruction importInstruction) {
        log.warn("Error when overriding chain {} by {}", importInstruction.getId(), importInstruction.getOverriddenBy());
        return ImportInstructionResult.builder()
                .id(importInstruction.getId())
                .name(importInstruction.getEntityName())
                .entityType(ImportEntityType.CHAIN)
                .status(ImportInstructionStatus.ERROR_ON_OVERRIDE)
                .errorMessage(e.getMessage())
                .build();
    }

    private void validateImportInstruction(ImportInstruction importInstruction) {
        ImportEntityType entityType = importInstruction.getEntityType();
        ImportInstructionAction action = importInstruction.getAction();

        boolean failed = false;
        String errorMessage = null;
        switch (entityType) {
            case CHAIN -> {
                failed = action == ImportInstructionAction.OVERRIDE && importInstruction.getOverriddenBy() == null;
                errorMessage = "Overridden By must not be null for instruction with the OVERRIDE action";
            }
            case SERVICE -> {
                failed = action == ImportInstructionAction.OVERRIDE;
                errorMessage = "Service instruction does not support the OVERRIDE action";

            }
            case SPECIFICATION_GROUP -> {
                failed = action == ImportInstructionAction.IGNORE || action == ImportInstructionAction.OVERRIDE;
                errorMessage = "Specification Group instruction does not support action IGNORE and OVERRIDE";
            }
            case SPECIFICATION -> {
                failed = action == ImportInstructionAction.IGNORE || action == ImportInstructionAction.OVERRIDE;
                errorMessage = "Specification instruction does not support action IGNORE and OVERRIDE";
            }
        }

        if (failed) {
            log.error(errorMessage);
            throw new ImportInstructionsValidationException(errorMessage);
        }
    }
}
