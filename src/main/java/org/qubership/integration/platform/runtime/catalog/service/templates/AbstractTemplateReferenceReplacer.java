package org.qubership.integration.platform.runtime.catalog.service.templates;

import java.util.Objects;

import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.catalog.persistence.configs.entity.template.Template;
import org.qubership.integration.platform.catalog.persistence.configs.repository.template.TemplateRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractTemplateReferenceReplacer {
    protected String TEMPLATE_NOT_FOUND_MESSAGE = "Unable to find a template (id=%s) for element (id=%s) of chain (id=%s)";
    
    protected final TemplateRepository templateRepository;

    protected abstract String getElementType();

    protected abstract String getTemplateReferencePropertyName();

    protected abstract String getTemplateBodyPropertyName();

    protected abstract boolean isTemplateReferencePresent(ChainElement chainElement);

    public abstract void replaceTemplateReferences(ChainElement chainElement);

    protected boolean isSuitable(ChainElement chainElement) {
        return Objects.equals(getElementType(), chainElement.getType()) && isTemplateReferencePresent(chainElement);
    }

    protected Template findTemplateByIdOrException(String templateId, ChainElement chainElement) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException(String.format(TEMPLATE_NOT_FOUND_MESSAGE, templateId,
                        chainElement.getId(), chainElement.getChain().getId())));
    }
}
