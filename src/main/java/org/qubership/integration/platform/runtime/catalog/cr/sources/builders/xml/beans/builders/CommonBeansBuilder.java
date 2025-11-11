package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders;

import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ElementBeansBuilder;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.stereotype.Component;

@Component
public class CommonBeansBuilder implements ElementBeansBuilder {
    @Override
    public boolean applicableTo(ChainElement element) {
        return true;
    }

    @Override
    public void build(XMLStreamWriter2 streamWriter, ChainElement element) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", element.getId());
        streamWriter.writeAttribute("type", "org.qubership.integration.platform.engine.metadata.ElementInfo");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.metadata.builders.ElementInfoBuilder");
        streamWriter.writeAttribute("builderMethod", "build");

        streamWriter.writeStartElement("properties");

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "id");
        streamWriter.writeAttribute("value", element.getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "name");
        streamWriter.writeAttribute("value", element.getName());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "type");
        streamWriter.writeAttribute("value", element.getType());

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }
}
