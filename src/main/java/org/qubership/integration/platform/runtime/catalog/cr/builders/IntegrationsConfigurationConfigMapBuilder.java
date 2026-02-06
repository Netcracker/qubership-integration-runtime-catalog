package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.cfg.IntegrationsConfiguration;
import org.qubership.integration.platform.runtime.catalog.cr.cfg.IntegrationsConfigurationBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class IntegrationsConfigurationConfigMapBuilder implements ResourceBuilder<List<Chain>> {
    public static final String CONTENT_KEY = "content";

    private final YAMLMapper resourceYamlMapper;
    private final YAMLMapper integrationsConfigurationMapper;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> namingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;
    private final IntegrationsConfigurationBuilder integrationsConfigurationBuilder;

    @Autowired
    public IntegrationsConfigurationConfigMapBuilder(
            @Qualifier("customResourceYamlMapper") YAMLMapper resourceYamlMapper,
            @Qualifier("integrationsConfigurationMapper") YAMLMapper integrationsConfigurationMapper,
            @Qualifier("integrationsConfigurationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> namingStrategy,
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy,
            IntegrationsConfigurationBuilder integrationsConfigurationBuilder
    ) {
        this.resourceYamlMapper = resourceYamlMapper;
        this.integrationsConfigurationMapper = integrationsConfigurationMapper;
        this.namingStrategy = namingStrategy;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.integrationsConfigurationBuilder = integrationsConfigurationBuilder;
    }

    @Override
    public boolean enabled(ResourceBuildContext<List<Chain>> context) {
        return context.getBuildInfo().getOptions().getIntegrations().isConfigurationConfigMapNeeded();
    }

    @Override
    public String build(ResourceBuildContext<List<Chain>> context) throws Exception {
        try {
            ObjectNode configMapNode = resourceYamlMapper.createObjectNode();
            configMapNode.set("apiVersion", configMapNode.textNode("v1"));
            configMapNode.set("kind", configMapNode.textNode("ConfigMap"));

            ObjectNode metadataNode = configMapNode.withObjectProperty("metadata");
            metadataNode.set("name", metadataNode.textNode(namingStrategy.getName(context)));

            String integrationName = integrationResourceNamingStrategy.getName(context.updateTo(Collections.emptyList()));
            metadataNode.withObject("labels")
                    .set("camel.apache.org/integration", metadataNode.textNode(integrationName));


            IntegrationsConfiguration integrationsConfiguration = integrationsConfigurationBuilder.build(context);
            String content = integrationsConfigurationMapper.writeValueAsString(integrationsConfiguration);
            configMapNode.withObjectProperty("data")
                    .set(CONTENT_KEY, configMapNode.textNode(content));

            return resourceYamlMapper.writeValueAsString(configMapNode);
        } catch (Exception e) {
            String message = "Failed to build integration source ConfigMap for chains configuration";
            log.error(message, e);
            throw new CustomResourceBuildError(message, e);
        }
    }
}
