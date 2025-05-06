package org.qubership.integration.platform.runtime.catalog.configuration;

import org.qubership.integration.platform.catalog.service.exportimport.ExportImportConstants;
import org.qubership.integration.platform.catalog.service.exportimport.ImportFilesExtractor;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.TemplateSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ExportImportConfiguration {
    @Bean
    public ImportFilesExtractor templateImportFilesExtractor() {
        return new ImportFilesExtractor(ExportImportConstants.TEMPLATE_YAML_NAME_PREFIX, TemplateSerializer.ARCH_PARENT_DIR);
    }

    @Bean
    public ImportFilesExtractor serviceImportFilesExtractor() {
        return new ImportFilesExtractor(ExportImportConstants.SERVICE_YAML_NAME_PREFIX, ExportImportConstants.ARCH_PARENT_DIR);
    }
}
