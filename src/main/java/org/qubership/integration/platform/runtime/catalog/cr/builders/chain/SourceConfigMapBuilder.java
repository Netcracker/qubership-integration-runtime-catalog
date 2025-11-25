package org.qubership.integration.platform.runtime.catalog.cr.builders.chain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilderFactory;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SourceConfigMapBuilder implements ResourceBuilder<Chain> {
    public static final String CONTENT_KEY = "content";

    private final YAMLMapper yamlMapper;
    private final IntegrationSourceBuilderFactory integrationSourceBuilderFactory;
    private final NamingStrategy<Chain> configMapNamingStrategy;

    @Autowired
    public SourceConfigMapBuilder(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            IntegrationSourceBuilderFactory integrationSourceBuilderFactory,
            NamingStrategy<Chain> configMapNamingStrategy
    ) {
        this.yamlMapper = yamlMapper;
        this.integrationSourceBuilderFactory = integrationSourceBuilderFactory;
        this.configMapNamingStrategy = configMapNamingStrategy;
    }

    @Override
    public ObjectNode build(Chain chain, ResourceBuildContext context) throws Exception {
        String language = context.getOptions().getLanguage();
        IntegrationSourceBuilder sourceBuilder = integrationSourceBuilderFactory.getBuilder(language);
        SourceBuilderContext sourceBuilderContext = createSourceBuilderContext(context);

        try {
            ObjectNode configMapNode = yamlMapper.createObjectNode();
            configMapNode.set("apiVersion", configMapNode.textNode("v1"));
            configMapNode.set("kind", configMapNode.textNode("ConfigMap"));

            ObjectNode metadataNode = configMapNode.withObjectProperty("metadata");
            metadataNode.set("name", metadataNode.textNode(configMapNamingStrategy.getName(chain)));

            configMapNode.withObjectProperty("data")
                    .set(CONTENT_KEY, configMapNode.textNode(sourceBuilder.build(chain, sourceBuilderContext)));

            return configMapNode;
        } catch (Exception e) {
            String message = String.format(
                    "Failed to build integration source for chain '%s' (%s)",
                    chain.getName(),
                    chain.getId()
            );
            log.error(message, e);
            throw new CustomResourceBuildError(message, e);
        }
    }

    private SourceBuilderContext createSourceBuilderContext(ResourceBuildContext context) {
        return SourceBuilderContext.builder()
                .buildVersion(context.getBuildVersion())
                .build();
    }
}
