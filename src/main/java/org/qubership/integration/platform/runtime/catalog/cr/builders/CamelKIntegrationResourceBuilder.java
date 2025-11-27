package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.qubership.integration.platform.runtime.catalog.cr.builders.chain.SourceConfigMapBuilder.CONTENT_KEY;

@Component
public class CamelKIntegrationResourceBuilder implements ResourceBuilder<List<Chain>> {
    private static final String MOUNT_DIR = "/etc/camel/sources/";

    private final YAMLMapper yamlMapper;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy;

    @Autowired
    public CamelKIntegrationResourceBuilder(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy,
            NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy
    ) {
        this.yamlMapper = yamlMapper;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.configMapNamingStrategy = configMapNamingStrategy;
    }

    @Override
    public ObjectNode build(ResourceBuildContext<List<Chain>> context) throws Exception {
        List<Chain> chains = context.getData();
        ResourceBuildOptions options = context.getBuildInfo().getOptions();

        ObjectNode crNode = yamlMapper.createObjectNode();
        crNode.set("apiVersion", crNode.textNode("camel.apache.org/v1"));
        crNode.set("kind", crNode.textNode("Integration"));

        ObjectNode metadataNode = crNode.withObjectProperty("metadata");
        metadataNode.set("name", metadataNode.textNode(integrationResourceNamingStrategy.getName(context)));

        ObjectNode specNode = crNode.withObjectProperty("spec");

        if (chains.isEmpty()) {
            throw new CustomResourceBuildError("Chain list is empty");
        }

        ObjectNode traitsNode = specNode.withObjectProperty("traits");

        traitsNode.withObjectProperty("container")
                .set("image", specNode.textNode(options.getImage()));

        ArrayNode resourcesNode = traitsNode
                .withObjectProperty("mount")
                .withArrayProperty("resources");

        chains.stream().map(chain -> {
            ResourceBuildContext<Chain> chainResourceBuildContext =
                    ResourceBuildContext.create(context.getBuildInfo(), chain);
            String name = configMapNamingStrategy.getName(chainResourceBuildContext);
            String resource = String.format("configmap:%s/%s@%s",
                    name, CONTENT_KEY, getMountPath(chainResourceBuildContext));
            return resourcesNode.textNode(resource);
        }).forEach(resourcesNode::add);

        ArrayNode arrayNode = traitsNode
                .withObjectProperty("camel")
                .withArrayProperty("properties");
        IntStream.range(0, chains.size())
                .mapToObj(index -> {
                    Chain chain = chains.get(index);
                    ResourceBuildContext<Chain> chainResourceBuildContext =
                            ResourceBuildContext.create(context.getBuildInfo(), chain);
                    String path = getMountPath(chainResourceBuildContext);
                    return List.of(
                            String.format("camel.k.sources[%d].language = %s", index, options.getLanguage()),
                            String.format("camel.k.sources[%d].location = file:%s", index, path),
                            String.format("camel.k.sources[%d].name = %s", index, chain.getName()),
                            String.format("camel.k.sources[%d].id = %s", index, chain.getId())
                    );
                })
                .flatMap(Collection::stream)
                .map(arrayNode::textNode).forEach(arrayNode::add);

        crNode.withObjectProperty("status");

        return crNode;
    }

    private String getMountPath(ResourceBuildContext<Chain> context) {
        String name = configMapNamingStrategy.getName(context);
        String fileName = String.format("%s.%s", name, context.getBuildInfo().getOptions().getLanguage());
        return Paths.get(MOUNT_DIR, fileName).toString();
    }
}
