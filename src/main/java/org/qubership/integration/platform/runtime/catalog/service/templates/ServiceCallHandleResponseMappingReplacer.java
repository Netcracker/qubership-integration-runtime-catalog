package org.qubership.integration.platform.runtime.catalog.service.templates;

import static org.qubership.integration.platform.catalog.model.constant.CamelOptions.AFTER;
import static org.qubership.integration.platform.catalog.model.constant.CamelOptions.TYPE;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.catalog.model.constant.CamelNames;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.catalog.persistence.configs.entity.template.Template;
import org.qubership.integration.platform.catalog.persistence.configs.repository.template.TemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class ServiceCallHandleResponseMappingReplacer extends AbstractMappingTemplateReferenceReplacer {

    public ServiceCallHandleResponseMappingReplacer(TemplateRepository templateRepository) {
        super(templateRepository);
    }

    @Override
    protected String getElementType() {
        return CamelNames.SERVICE_CALL_COMPONENT;
    }

    @Override
    protected boolean isTemplateReferencePresent(ChainElement chainElement) {
        return getStreamOfElementsWithTemplateReference(chainElement).count() > 0;
    }

    @Override
    public void replaceTemplateReferences(ChainElement chainElement) {
        getStreamOfElementsWithTemplateReference(chainElement)
                .forEach(afterElement -> {
                    String templateId = (String) afterElement.get(getTemplateReferencePropertyName());
                    Template template = findTemplateByIdOrException(templateId, chainElement);

                    afterElement.remove(getTemplateReferencePropertyName());
                    afterElement.put(getTemplateBodyPropertyName(), template.getProperties());
                });
    }

    private Stream<Map<String, Object>> getStreamOfElementsWithTemplateReference(ChainElement chainElement)  {
        return CollectionUtils.emptyIfNull((List<Map<String, Object>>) chainElement.getProperties().get(AFTER)).stream()
                .filter(afterElement -> StringUtils.isNotBlank((String) afterElement.get(getTemplateReferencePropertyName())))
                .filter(afterElement -> CamelNames.MAPPER_2.equals((String) afterElement.get(TYPE)));
    }
}
