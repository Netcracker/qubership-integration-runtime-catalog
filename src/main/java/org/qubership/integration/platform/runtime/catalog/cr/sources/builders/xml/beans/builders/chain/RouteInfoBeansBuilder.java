package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.chain;

import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ChainBeanBuilder;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.DeploymentRoute;
import org.qubership.integration.platform.runtime.catalog.service.RoutesGetterService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import javax.xml.stream.XMLStreamException;

@Component
public class RouteInfoBeansBuilder implements ChainBeanBuilder {
    private final RoutesGetterService routesGetterService;

    public RouteInfoBeansBuilder(RoutesGetterService routesGetterService) {
        this.routesGetterService = routesGetterService;
    }

    @Override
    public void build(XMLStreamWriter2 streamWriter, Chain chain, SourceBuilderContext context) throws Exception {
        Collection<DeploymentRoute> routes = routesGetterService.getRoutes((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("chain").get("id"), chain.getId()));
        for (DeploymentRoute route : routes) {
            addRouteRegistrationInfoBean(streamWriter, chain, route);
        }
    }

    private void addRouteRegistrationInfoBean(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            DeploymentRoute route
    ) throws XMLStreamException {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", "RouteRegistrationInfo-" + route.getId());
        streamWriter.writeAttribute("type", "org.qubership.integration.platform.engine.metadata.RouteRegistrationInfo");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.metadata.builders.RouteRegistrationInfoBuilder");
        streamWriter.writeAttribute("builderMethod", "build");


        streamWriter.writeStartElement("properties");

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "chainId");
        streamWriter.writeAttribute("value", chain.getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "path");
        streamWriter.writeAttribute("value", route.getPath());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "gatewayPrefix");
        streamWriter.writeAttribute("value", String.valueOf(route.getGatewayPrefix()));

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "variableName");
        streamWriter.writeAttribute("value", String.valueOf(route.getVariableName()));

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "type");
        streamWriter.writeAttribute("value", route.getType().name());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "connectTimeout");
        streamWriter.writeAttribute("value", Long.toString(route.getConnectTimeout()));

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }
}
