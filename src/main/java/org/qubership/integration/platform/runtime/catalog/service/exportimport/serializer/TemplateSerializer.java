package org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.qubership.integration.platform.catalog.persistence.configs.entity.template.Template;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.ExportedTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateSerializer {
    public static final String ARCH_PARENT_DIR = "templates";

    private final YAMLMapper yamlMapper;
    private final ExportableObjectWriterVisitor exportableObjectWriterVisitor;

    public ExportedTemplate serialize(Template template) {
        ObjectNode templateNode = yamlMapper.valueToTree(template);

        return new ExportedTemplate(template.getId(), templateNode);
    }

    public byte[] writeSerializedArchive(List<ExportedTemplate> exportedTemplates) {
        try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                for (ExportedTemplate exportedTemplate : exportedTemplates) {
                    String entryPath = ARCH_PARENT_DIR + File.separator + exportedTemplate.getId() + File.separator;
                    exportedTemplate.accept(exportableObjectWriterVisitor, zipOut, entryPath);
                }
            }
            return fos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unknown exception while archive creation: " + e.getMessage(), e);
        }
    }
}
