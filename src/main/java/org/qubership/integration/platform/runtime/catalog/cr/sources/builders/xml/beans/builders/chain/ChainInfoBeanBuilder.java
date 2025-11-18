package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.chain;

import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ChainBeanBuilder;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.stereotype.Component;

@Component
public class ChainInfoBeanBuilder implements ChainBeanBuilder {
    @Override
    public void build(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            SourceBuilderContext context
    ) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", "ChainInfo-" + chain.getId());
        streamWriter.writeAttribute("type", "org.qubership.integration.platform.engine.metadata.ChainInfo");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.metadata.builders.ChainInfoBuilder");
        streamWriter.writeAttribute("builderMethod", "build");

        streamWriter.writeStartElement("properties");

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "id");
        streamWriter.writeAttribute("value", chain.getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "name");
        streamWriter.writeAttribute("value", chain.getName());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "version");
        streamWriter.writeAttribute("value", context.getBuildVersion());

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }
}
