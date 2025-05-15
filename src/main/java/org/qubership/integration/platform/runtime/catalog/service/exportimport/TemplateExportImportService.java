package org.qubership.integration.platform.runtime.catalog.service.exportimport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.qubership.integration.platform.catalog.persistence.configs.entity.AbstractEntity;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.ActionLog;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.EntityType;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.LogOperation;
import org.qubership.integration.platform.catalog.persistence.configs.entity.template.Template;
import org.qubership.integration.platform.catalog.persistence.configs.repository.template.TemplateRepository;
import org.qubership.integration.platform.catalog.service.ActionsLogService;
import org.qubership.integration.platform.catalog.service.TemplateBaseService;
import org.qubership.integration.platform.catalog.service.exportimport.ImportFilesExtractor;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.ExportedTemplate;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.ImportTemplateResult;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.ImportTemplateStatus;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.TemplateDeserializationResult;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.TemplateSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.extern.slf4j.Slf4j;

import static org.qubership.integration.platform.catalog.service.exportimport.ExportImportConstants.ZIP_EXTENSION;
import static org.qubership.integration.platform.catalog.service.exportimport.ExportImportUtils.*;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@Service
@Slf4j
@Transactional
public class TemplateExportImportService {
    private final TransactionTemplate transactionTemplate;
    private final YAMLMapper yamlMapper;
    private final TemplateRepository templateRepository;
    private final TemplateBaseService templateService;
    private final TemplateSerializer templateSerializer;
    private final ImportFilesExtractor importFilesExtractor;
    protected final ActionsLogService actionLogger;

    public TemplateExportImportService(TransactionTemplate transactionTemplate,
            YAMLMapper yamlMapper,
            TemplateRepository templateRepository,
            TemplateBaseService templateBaseService,
            TemplateSerializer templateSerializer,
            @Qualifier("templateImportFilesExtractor") ImportFilesExtractor importFilesExtractor,
            ActionsLogService actionLogger) {
        this.transactionTemplate = transactionTemplate;
        this.yamlMapper = yamlMapper;
        this.templateRepository = templateRepository;
        this.templateService = templateBaseService;
        this.templateSerializer = templateSerializer;
        this.importFilesExtractor = importFilesExtractor;
        this.actionLogger = actionLogger;
    }

    public byte[] exportTemplates(List<String> templateIds) {
        List<Template> templates = new ArrayList<>();
        if (CollectionUtils.isEmpty(templateIds)) {
            templates.addAll(templateRepository.findAll());
        } else {
            templates.addAll(templateRepository.findByIds(templateIds));
        }

        if (templates.isEmpty()) {
            return null;
        }
        List<ExportedTemplate> exportedTemplates = templates.stream().map(this::exportTemplate).toList();
        byte[] archive = templateSerializer.writeSerializedArchive(exportedTemplates);

        return archive;
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public List<ImportTemplateResult> importTemplates(MultipartFile importFile, List<String> templateIds) {
        String fileExtension = FilenameUtils.getExtension(importFile.getOriginalFilename());
        logTemplateExportImport(null, importFile.getOriginalFilename(), LogOperation.IMPORT);
        if (ZIP_EXTENSION.equalsIgnoreCase(fileExtension)) {
            String exportDirectory = Paths.get(FileUtils.getTempDirectory().getAbsolutePath(),
                    UUID.randomUUID().toString()).toString();
            List<File> templateFiles;

            try (InputStream fs = importFile.getInputStream()) {
                templateFiles = importFilesExtractor.extractFromZip(fs, exportDirectory);
            } catch (IOException e) {
                deleteFile(exportDirectory);
                throw new RuntimeException("Unexpected error while archive unpacking: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                deleteFile(exportDirectory);
                throw e;
            }

            List<ImportTemplateResult> result = new ArrayList<>();
            for (File templateFile : templateFiles) {
                String templateId = importFilesExtractor.extractEntityIdFromFileName(templateFile);
                if (CollectionUtils.isNotEmpty(templateIds) && !templateIds.contains(templateId)) {
                    result.add(ImportTemplateResult.builder()
                            .id(templateId)
                            .name(templateId)
                            .status(ImportTemplateStatus.IGNORED)
                            .build());
                    log.info("Template {} ignored as a part of import exclusion list", templateId);
                    continue;
                }
                ImportTemplateResult importTemplateResult = importTemplateInTransaction(templateFile);
                result.add(importTemplateResult);
            }
            return result;
        } else {
            throw new RuntimeException("Unsupported file extension: " + fileExtension);
        }
    }

    protected synchronized ImportTemplateResult importTemplateInTransaction(File templateFile) {
        ImportTemplateResult result;
        Optional<Template> baseTemplateOptional = Optional.empty();

        try {
            ObjectNode templateNode = getFileNode(templateFile);
            TemplateDeserializationResult deserializationResult = buildBaseTemplate(templateNode);
            baseTemplateOptional = Optional.ofNullable(deserializationResult.getTemplate());

            result = transactionTemplate.execute((status) -> {

                deserializationResult.setTemplate(deserializeTemplate(templateNode, templateFile.getParentFile()));

                StringBuilder message = new StringBuilder();
                ImportTemplateStatus importStatus = createOrUpdateExistingTemplate(deserializationResult, message::append);

                return ImportTemplateResult.builder()
                        .id(deserializationResult.getTemplate().getId())
                        .name(deserializationResult.getTemplate().getName())
                        .status(importStatus)
                        .message(message.toString())
                        .build();
            });
        } catch (Exception e) {
            result = ImportTemplateResult.builder()
                    .id(baseTemplateOptional.map(Template::getId).orElse(null))
                    .name(baseTemplateOptional.map(Template::getName).orElse(null))
                    .status(ImportTemplateStatus.ERROR)
                    .message(e.getMessage())
                    .build();
            log.error("An error occurred while importing template {} ({})", result.getName(), result.getId(), e);
        }

        return result;
    }

    private TemplateDeserializationResult buildBaseTemplate(ObjectNode templateNode) {
        String templateId = Optional.ofNullable(templateNode.get(AbstractEntity.Fields.id))
                .map(jsonNode -> jsonNode.asText(null))
                .orElseThrow(() -> new RuntimeException("Missing id field in template file"));

        Template baseTemplate = Template.builder()
                .id(templateId)
                .name(Optional.ofNullable(templateNode.get(AbstractEntity.Fields.name))
                        .map(jsonNode -> jsonNode.asText(null)).orElse(null))
                .build();

        return new TemplateDeserializationResult(baseTemplate);
    }

    private Template deserializeTemplate(ObjectNode templateNode, File templateDirectory) {
        try {
            return yamlMapper.treeToValue(templateNode, Template.class);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while deserializing template", e);
        }
    }

    protected ObjectNode getFileNode(File file) throws IOException {
        return (ObjectNode) yamlMapper.readTree(file);
    }

    private ImportTemplateStatus createOrUpdateExistingTemplate(TemplateDeserializationResult deserializationResult,
            Consumer<String> messageHandler) {
        Template template = deserializationResult.getTemplate();
        ImportTemplateStatus status;

        Optional<Template> oldTemplate = templateRepository.findById(template.getId());

        if (oldTemplate.isPresent()) {
            status = ImportTemplateStatus.UPDATED;
            templateService.update(template);
        } else {
            status = ImportTemplateStatus.CREATED;
            templateService.create(template, true);
        }

        return status;
    }

    private ExportedTemplate exportTemplate(Template template) {
        try {
            ExportedTemplate serializedTemplate = templateSerializer.serialize(template);
            logTemplateExportImport(template, null, LogOperation.EXPORT);
            return serializedTemplate;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error while serializing template: %s",  template.getId()), e);
        }
    }

    private void logTemplateExportImport(Template template, String archiveName, LogOperation operation) {
        actionLogger.logAction(ActionLog.builder()
                .entityType(template != null ? EntityType.getTemplateType(template) : EntityType.TEMPLATES)
                .entityId(template != null ? template.getId() : null)
                .entityName(template != null ? template.getName() : archiveName)
                .operation(operation)
                .build());
    }
}
