package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ContainerOptions;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

import static org.qubership.integration.platform.runtime.catalog.cr.builders.chain.SourceConfigMapBuilder.CONTENT_KEY;

@Component
public class CamelKIntegrationResourceBuilder implements ResourceBuilder<List<Chain>> {
    private static final Map<String, String> DEFAULT_ENVIRONMENT = Map.of(
            "CONSUL_URL", "{{ .Values.consul.url }}",
            "CONSUL_ADMIN_TOKEN", "{{ .Values.consul.adminToken }}",
            "OPENSEARCH_HOST", "{{ .Values.opensearch.host }}",
            "OPENSEARCH_PORT", "{{ .Values.opensearch.port }}",
            "POSTGRES_URL", "{{ .Values.postgres.url }}",
            "POSTGRES_USER", "{{ .Values.postgres.user }}",
            "POSTGRES_PASSWORD", "{{ .Values.postgres.password }}"
    );
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
        ObjectNode crNode = yamlMapper.createObjectNode();
        crNode.set("apiVersion", crNode.textNode("camel.apache.org/v1"));
        crNode.set("kind", crNode.textNode("Integration"));

        ObjectNode metadataNode = crNode.withObjectProperty("metadata");
        metadataNode.set("name", metadataNode.textNode(integrationResourceNamingStrategy.getName(context)));

        ObjectNode specNode = crNode.withObjectProperty("spec");

        if (context.getData().isEmpty()) {
            throw new CustomResourceBuildError("Chain list is empty");
        }

        ObjectNode traitsNode = specNode.withObjectProperty("traits");

        addContainerTraits(traitsNode, context);
        addMountTraits(traitsNode, context);
        addCamelPropertiesTraits(traitsNode, context);
        addEnvironmentVarsTraits(traitsNode, context);

        crNode.withObjectProperty("status");

        return crNode;
    }

    private void addContainerTraits(ObjectNode traitsNode, ResourceBuildContext<List<Chain>> context) {
        ContainerOptions containerOptions = context.getBuildInfo().getOptions().getContainer();
        String image = containerOptions.getImage();
        if (StringUtils.isBlank(image)) {
            image = "{{ .Values.container.image }}";
        }
        ObjectNode containerNode = traitsNode.withObjectProperty("container");
        containerNode.set("image", traitsNode.textNode(image));
        containerNode.set("imagePullPolicy", traitsNode.textNode(containerOptions.getImagePoolPolicy().name()));
    }

    private void addMountTraits(ObjectNode traitsNode, ResourceBuildContext<List<Chain>> context) {
        ArrayNode resourcesNode = traitsNode
                .withObjectProperty("mount")
                .withArrayProperty("resources");

        context.getData().stream().map(chain -> {
            ResourceBuildContext<Chain> chainResourceBuildContext = context.updateTo(chain);
            String name = configMapNamingStrategy.getName(chainResourceBuildContext);
            String resource = String.format("configmap:%s/%s@%s",
                    name, CONTENT_KEY, getMountPath(chainResourceBuildContext));
            return resourcesNode.textNode(resource);
        }).forEach(resourcesNode::add);
    }

    private void addCamelPropertiesTraits(ObjectNode traitsNode, ResourceBuildContext<List<Chain>> context) {
        List<Chain> chains = context.getData();
        ResourceBuildOptions options = context.getBuildInfo().getOptions();
        ArrayNode arrayNode = traitsNode
                .withObjectProperty("camel")
                .withArrayProperty("properties");
        IntStream.range(0, chains.size())
                .mapToObj(index -> {
                    Chain chain = chains.get(index);
                    String path = getMountPath(context.updateTo(chain));
                    return List.of(
                            String.format("camel.k.sources[%d].language = %s", index, options.getLanguage()),
                            String.format("camel.k.sources[%d].location = file:%s", index, path),
                            String.format("camel.k.sources[%d].name = %s", index, chain.getName()),
                            String.format("camel.k.sources[%d].id = %s", index, chain.getId())
                    );
                })
                .flatMap(Collection::stream)
                .map(arrayNode::textNode).forEach(arrayNode::add);
    }

    private String getMountPath(ResourceBuildContext<Chain> context) {
        String name = configMapNamingStrategy.getName(context);
        String fileName = String.format("%s.%s", name, context.getBuildInfo().getOptions().getLanguage());
        return Paths.get(MOUNT_DIR, fileName).toString();
    }

    private void addEnvironmentVarsTraits(ObjectNode traitsNode, ResourceBuildContext<List<Chain>> context) {
        ArrayNode varsNode = traitsNode
                .withObjectProperty("environment")
                .withArrayProperty("vars");
        Map<String, String> environment = new HashMap<>(DEFAULT_ENVIRONMENT);
        environment.putAll(context.getBuildInfo().getOptions().getEnvironment());
        environment
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .map(varsNode::textNode)
                .forEach(varsNode::add);
    }
}
