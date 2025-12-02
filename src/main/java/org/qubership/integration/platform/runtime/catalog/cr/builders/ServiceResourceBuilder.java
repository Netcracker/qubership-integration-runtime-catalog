package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ServiceResourceBuilder  implements ResourceBuilder<List<Chain>> {
    private final YAMLMapper yamlMapper;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;

    @Autowired
    public ServiceResourceBuilder(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            @Qualifier("serviceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy,
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy
    ) {
        this.yamlMapper = yamlMapper;
        this.serviceNamingStrategy = serviceNamingStrategy;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
    }

    @Override
    public String build(ResourceBuildContext<List<Chain>> context) throws Exception {
        try {
            ObjectNode serviceNode = yamlMapper.createObjectNode();
            serviceNode.set("apiVersion", serviceNode.textNode("v1"));
            serviceNode.set("kind", serviceNode.textNode("Service"));

            String integrationName = integrationResourceNamingStrategy.getName(context);
            String serviceName = serviceNamingStrategy.getName(context);

            ObjectNode metadataNode = serviceNode.withObjectProperty("metadata");
            metadataNode.set("name", metadataNode.textNode(serviceName));
            ObjectNode labelsNode = metadataNode.withObject("labels");
            labelsNode.set("camel.apache.org/integration", labelsNode.textNode(integrationName));
            labelsNode.set("domain", labelsNode.textNode(integrationName));

            ObjectNode specNode = serviceNode.withObjectProperty("spec");

            ObjectNode selectorNode = specNode.withObjectProperty("selector");
            selectorNode.set("camel.apache.org/integration", selectorNode.textNode(integrationName));

            ArrayNode portsNode = specNode.withArrayProperty("ports");
            ObjectNode portNode = portsNode.addObject();
            portNode.set("name", portNode.textNode("http"));
            portNode.set("protocol", portNode.textNode("TCP"));
            portNode.set("port", portNode.numberNode(80));
            portNode.set("targetPort", portNode.numberNode(8080));

            return yamlMapper.writeValueAsString(serviceNode);
        } catch (Exception e) {
            String message = "Failed to build Service resource";
            log.error(message, e);
            throw new CustomResourceBuildError(message, e);
        }
    }
}
