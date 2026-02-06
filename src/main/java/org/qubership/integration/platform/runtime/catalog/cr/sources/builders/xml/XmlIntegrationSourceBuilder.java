package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml;

import com.ctc.wstx.stax.WstxOutputFactory;
import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.builder.ChainRouteBuilder;
import org.qubership.integration.platform.runtime.catalog.builder.XmlBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ChainBeanBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ElementBeansBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ElementBeansBuilderFactory;
import org.qubership.integration.platform.runtime.catalog.model.ChainRoute;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

@Component
public class XmlIntegrationSourceBuilder implements IntegrationSourceBuilder {
    private static final String SCHEMA = "https://camel.apache.org/schema/xml-io";
    private static final String CAMEL = "camel";

    private final ChainRouteBuilder chainRouteBuilder;
    private final XmlBuilder xmlBuilder;
    private final ElementBeansBuilderFactory elementBeansBuilderFactory;
    private final Collection<ChainBeanBuilder> chainBeanBuilders;

    @Autowired
    public XmlIntegrationSourceBuilder(
            ChainRouteBuilder chainRouteBuilder,
            XmlBuilder xmlBuilder,
            Collection<ChainBeanBuilder> chainBeanBuilders,
            ElementBeansBuilderFactory elementBeansBuilderFactory
    ) {
        this.chainRouteBuilder = chainRouteBuilder;
        this.xmlBuilder = xmlBuilder;
        this.chainBeanBuilders = chainBeanBuilders;
        this.elementBeansBuilderFactory = elementBeansBuilderFactory;
    }

    @Override
    public String getLanguageName() {
        return "xml";
    }

    @Override
    public String build(Chain chain, SourceBuilderContext context) throws Exception {
        return buildContent(chain, context);
    }

    private String buildContent(Chain chain, SourceBuilderContext context) throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter2 streamWriter = (XMLStreamWriter2) new WstxOutputFactory().createXMLStreamWriter(result);
        streamWriter.writeStartDocument();
        streamWriter.writeStartElement(CAMEL);
        streamWriter.writeDefaultNamespace(SCHEMA);

        List<ChainRoute> routes = chainRouteBuilder.build(chain.getElements());
        routes.forEach(route -> route.setGroup(chain.getId()));

        writeBeans(streamWriter, chain, context);
        xmlBuilder.buildRoutesContent(streamWriter, routes);

        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();
        streamWriter.flush();
        streamWriter.close();

        return result.toString();
    }

    private void writeBeans(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            SourceBuilderContext context
    ) throws Exception {
        writeChainBeans(streamWriter, chain, context);
        for (ChainElement element : chain.getElements()) {
            writeChainElementBeans(streamWriter, element, context);
        }
    }

    private void writeChainBeans(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            SourceBuilderContext context
    ) throws Exception {
        for (ChainBeanBuilder builder : chainBeanBuilders) {
            builder.build(streamWriter, chain, context);
        }
    }

    private void writeChainElementBeans(
            XMLStreamWriter2 streamWriter,
            ChainElement element,
            SourceBuilderContext context
    ) throws Exception {
        ElementBeansBuilder builder = elementBeansBuilderFactory.getElementBeansBuilder(element);
        builder.build(streamWriter, element, context);
    }
}
