package org.qubership.integration.platform.runtime.catalog.service.templates;

import java.util.List;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChainElementTemplateReferenceReplacer {
    private final List<AbstractTemplateReferenceReplacer> templateReferenceReplacers;

    public void replaceTemplateReferences(List<ChainElement> chainElements) {
        for (ChainElement element: chainElements) {
            replaceTemplateReferences(element);
        }
    }

    public void replaceTemplateReferences(ChainElement chainElement) {
        for (AbstractTemplateReferenceReplacer replacer : templateReferenceReplacers) {
            if (replacer.isSuitable(chainElement)) {
                replacer.replaceTemplateReferences(chainElement);
            }
        }
    }
}
