package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml;

import com.ctc.wstx.stax.WstxOutputFactory;
import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.builder.ChainRouteBuilder;
import org.qubership.integration.platform.runtime.catalog.builder.XmlBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilder;
import org.qubership.integration.platform.runtime.catalog.model.ChainRoute;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.List;

@Component
public class XmlIntegrationSourceBuilder implements IntegrationSourceBuilder {
    private static final String SCHEMA = "https://camel.apache.org/schema/xml-io";
    private static final String CAMEL = "camel";

    private final ChainRouteBuilder chainRouteBuilder;
    private final XmlBuilder xmlBuilder;

    @Autowired
    public XmlIntegrationSourceBuilder(
            ChainRouteBuilder chainRouteBuilder,
            XmlBuilder xmlBuilder
    ) {
        this.chainRouteBuilder = chainRouteBuilder;
        this.xmlBuilder = xmlBuilder;
    }

    @Override
    public String getLanguageName() {
        return "xml";
    }

    @Override
    public String build(Chain chain) throws Exception {
        return buildContent(chain);
    }

    private String buildContent(Chain chain) throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter2 streamWriter = (XMLStreamWriter2) new WstxOutputFactory().createXMLStreamWriter(result);
        streamWriter.writeStartDocument();
        streamWriter.writeStartElement(CAMEL);
        streamWriter.writeDefaultNamespace(SCHEMA);

        List<ChainRoute> routes = chainRouteBuilder.build(chain.getElements());
        routes.forEach(route -> route.setGroup(chain.getId()));

        writeBeans(streamWriter, chain);
        xmlBuilder.buildRoutesContent(streamWriter, routes);

        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();
        streamWriter.flush();
        streamWriter.close();

        return result.toString();
    }

    private void writeBeans(XMLStreamWriter2 streamWriter, Chain chain) throws Exception {
        writeChainMetadata(streamWriter, chain);
        for (ChainElement element : chain.getElements()) {
            writeChainElementBeans(streamWriter, element);
        }
    }

    private void writeChainMetadata(XMLStreamWriter2 streamWriter, Chain chain) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", chain.getId());
        streamWriter.writeAttribute("type", "org.qubership.integration.platform.engine.metadata.ChainInfo");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.metadata.ChainInfo.ChainInfoBuilder");
        streamWriter.writeAttribute("builderMethod", "build");

        streamWriter.writeStartElement("properties");

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "chainId");
        streamWriter.writeAttribute("value", chain.getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "chainName");
        streamWriter.writeAttribute("value", chain.getName());

        streamWriter.writeEndElement();

        streamWriter.writeEmptyElement("constructors");
        streamWriter.writeEndElement();
    }

    private void writeChainElementBeans(XMLStreamWriter2 streamWriter, ChainElement element) throws Exception {
        // TODO
    }
}
