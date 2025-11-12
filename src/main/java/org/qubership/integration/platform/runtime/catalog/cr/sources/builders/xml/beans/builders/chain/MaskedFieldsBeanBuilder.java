package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ChainBeanBuilder;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.MaskedField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MaskedFieldsBeanBuilder implements ChainBeanBuilder {
    private final ObjectMapper objectMapper;

    @Autowired
    public MaskedFieldsBeanBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void build(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            SourceBuilderContext context
    ) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", chain.getId());
        streamWriter.writeAttribute("type", "org.qubership.integration.platform.engine.metadata.MaskedFields");
        streamWriter.writeAttribute("factoryMethod", "fromJsonString");

        streamWriter.writeStartElement("constructors");

        streamWriter.writeEmptyElement("constructor");
        streamWriter.writeAttribute("index", "0");
        streamWriter.writeAttribute("value", getMaskedFieldsValue(chain));

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private String getMaskedFieldsValue(Chain chain) throws Exception {
        Set<String> fields = chain.getMaskedFields().stream()
                .map(MaskedField::getName)
                .collect(Collectors.toSet());
        return objectMapper.writeValueAsString(fields);
    }
}
