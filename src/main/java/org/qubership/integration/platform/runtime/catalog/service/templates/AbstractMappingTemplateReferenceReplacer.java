package org.qubership.integration.platform.runtime.catalog.service.templates;

import org.qubership.integration.platform.catalog.persistence.configs.repository.template.TemplateRepository;

public abstract class AbstractMappingTemplateReferenceReplacer extends AbstractTemplateReferenceReplacer {
    public AbstractMappingTemplateReferenceReplacer(TemplateRepository templateRepository) {
        super(templateRepository);
    }

    @Override
    protected String getTemplateReferencePropertyName() {
        return "mappingTemplateId";
    }

    @Override
    protected String getTemplateBodyPropertyName() {
        return "mappingDescription";
    }
}
