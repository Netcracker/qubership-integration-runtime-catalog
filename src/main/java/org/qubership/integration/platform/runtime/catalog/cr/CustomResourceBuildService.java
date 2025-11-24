package org.qubership.integration.platform.runtime.catalog.cr;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.CustomResourceOptions;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.IntegrationSourceBuilderFactory;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
public class CustomResourceBuildService {
    private static final String CONTENT_KEY = "content";
    private static final String MOUNT_DIR = "/etc/camel/sources/";
    private final YAMLMapper yamlMapper;
    private final NamingStrategy<Void> integrationResourceNamingStrategy;
    private final NamingStrategy<Chain> configMapNamingStrategy;
    private final NamingStrategy<CustomResourceOptions> buildVersionNamingStrategy;
    private final IntegrationSourceBuilderFactory integrationSourceBuilderFactory;

    @Autowired
    public CustomResourceBuildService(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            NamingStrategy<Void> integrationResourceNamingStrategy,
            NamingStrategy<Chain> configMapNamingStrategy,
            NamingStrategy<CustomResourceOptions> buildVersionNamingStrategy,
            IntegrationSourceBuilderFactory integrationSourceBuilderFactory
    ) {
        this.yamlMapper = yamlMapper;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.configMapNamingStrategy = configMapNamingStrategy;
        this.integrationSourceBuilderFactory = integrationSourceBuilderFactory;
        this.buildVersionNamingStrategy = buildVersionNamingStrategy;
    }

    public String buildCustomResource(
            List<Chain> chains,
            CustomResourceOptions options
    ) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (SequenceWriter sequenceWriter = yamlMapper.writer().writeValues(outputStream)) {
            SourceBuilderContext sourceBuilderContext = createSourceBuilderContext(options);
            for (Chain chain : chains) {
                buildResourcesForChain(sequenceWriter, chain, options, sourceBuilderContext);
            }
            sequenceWriter.write(buildIntegrationResource(chains, options));
            outputStream.flush();
            return outputStream.toString();
        } catch (IOException e) {
            log.error("Failed to build custom resource", e);
            throw new CustomResourceBuildError("Failed to build custom resource", e);
        }
    }

    private ObjectNode buildIntegrationResource(List<Chain> chains, CustomResourceOptions options) {
        ObjectNode crNode = yamlMapper.createObjectNode();
        crNode.set("apiVersion", crNode.textNode("camel.apache.org/v1"));
        crNode.set("kind", crNode.textNode("Integration"));

        ObjectNode metadataNode = crNode.withObjectProperty("metadata");
        metadataNode.set("name", metadataNode.textNode(integrationResourceNamingStrategy.getName(null)));

        ObjectNode specNode = crNode.withObjectProperty("spec");

        if (chains.isEmpty()) {
            throw new CustomResourceBuildError("Snapshot list is empty");
        }

        ObjectNode traitsNode = specNode.withObjectProperty("traits");

        traitsNode.withObjectProperty("container")
                    .set("image", specNode.textNode(options.getImage()));

        ArrayNode resourcesNode = traitsNode
                .withObjectProperty("mount")
                .withArrayProperty("resources");

        chains.stream().map(chain -> {
            String name = configMapNamingStrategy.getName(chain);
            String resource = String.format("configmap:%s/%s@%s",
                    name, CONTENT_KEY, getMountPath(chain, options));
            return resourcesNode.textNode(resource);
        }).forEach(resourcesNode::add);

        ArrayNode arrayNode = traitsNode
                .withObjectProperty("camel")
                .withArrayProperty("properties");
        IntStream.range(0, chains.size())
                .mapToObj(index -> {
                    Chain chain = chains.get(index);
                    String name = configMapNamingStrategy.getName(chains.get(index));
                    String path = getMountPath(chain, options);
                    return List.of(
                            String.format("camel.k.sources[%d].language = %s", index, options.getLanguage()),
                            String.format("camel.k.sources[%d].location = file:%s", index, path),
                            String.format("camel.k.sources[%d].name = %s", index, name)
                    );
                })
                .flatMap(Collection::stream)
                .map(arrayNode::textNode).forEach(arrayNode::add);

        crNode.withObjectProperty("status");

        return crNode;
    }

    private void buildResourcesForChain(
            SequenceWriter sequenceWriter,
            Chain chain,
            CustomResourceOptions options,
            SourceBuilderContext sourceBuilderContext
    ) throws IOException {
        sequenceWriter.write(buildSourceConfigMap(chain, options.getLanguage(), sourceBuilderContext));
    }

    private ObjectNode buildSourceConfigMap(
            Chain chain,
            String language,
            SourceBuilderContext context
    ) throws CustomResourceBuildError {
        IntegrationSourceBuilder sourceBuilder = integrationSourceBuilderFactory.getBuilder(language);

        try {
            ObjectNode configMapNode = yamlMapper.createObjectNode();
            configMapNode.set("apiVersion", configMapNode.textNode("v1"));
            configMapNode.set("kind", configMapNode.textNode("ConfigMap"));

            ObjectNode metadataNode = configMapNode.withObjectProperty("metadata");
            metadataNode.set("name", metadataNode.textNode(configMapNamingStrategy.getName(chain)));

            configMapNode.withObjectProperty("data")
                    .set(CONTENT_KEY, configMapNode.textNode(sourceBuilder.build(chain, context)));

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

    private String getMountPath(Chain chain, CustomResourceOptions options) {
        String name = configMapNamingStrategy.getName(chain);
        String fileName = String.format("%s.%s", name, options.getLanguage());
        return Paths.get(MOUNT_DIR, fileName).toString();
    }

    private SourceBuilderContext createSourceBuilderContext(CustomResourceOptions options) {
        return SourceBuilderContext.builder()
                .buildVersion(buildVersionNamingStrategy.getName(options))
                .build();
    }
}
