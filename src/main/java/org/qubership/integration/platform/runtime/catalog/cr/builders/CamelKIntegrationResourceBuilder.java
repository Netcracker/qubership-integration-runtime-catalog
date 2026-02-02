package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildError;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.builders.chain.SourceConfigMapBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.locations.SourceMountPointGetter;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ContainerOptions;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CamelKIntegrationResourceBuilder implements ResourceBuilder<List<Chain>> {
    private static final String QIP_CHAINS_CONFIGURATION_PATH = "/etc/integrations-config.yaml";

    private static final Map<String, String> DEFAULT_ENVIRONMENT = Map.of(
            "CONSUL_URL", "{{ .Values.consul.url }}",
            "CONSUL_ADMIN_TOKEN", "{{ .Values.consul.adminToken }}",
            "OPENSEARCH_HOST", "{{ .Values.opensearch.host }}",
            "OPENSEARCH_PORT", "{{ .Values.opensearch.port }}",
            "POSTGRES_URL", "{{ .Values.postgres.url }}",
            "POSTGRES_USER", "{{ .Values.postgres.user }}",
            "POSTGRES_PASSWORD", "{{ .Values.postgres.password }}",
            "MONITORING_ENABLED", "{{ .Values.monitoring.enabled }}"
    );
    private static final String TEMPLATE_NAME = "integration";

    @Data
    @Builder
    private static class ContainerData {
        String image;
        String imagePullPolicy;
    }

    @Data
    @Builder
    private static class TemplateData {
        private String name;
        private ContainerData container;
        private Collection<String> resources;
        private Collection<String> properties;
        private Collection<String> environment;
        private boolean propertiesEnabled;
        private String serviceAccountName;
    }

    private final Handlebars templates;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationsConfigurationConfigMapNamingStrategy;
    private final SourceMountPointGetter sourceMountPointGetter;

    @Autowired
    public CamelKIntegrationResourceBuilder(
            @Qualifier("customResourceTemplates") Handlebars templates,
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy,
            @Qualifier("serviceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy,
            @Qualifier("integrationsConfigurationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationsConfigurationConfigMapNamingStrategy,
            NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy,
            SourceMountPointGetter sourceMountPointGetter
    ) {
        this.templates = templates;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.serviceNamingStrategy = serviceNamingStrategy;
        this.configMapNamingStrategy = configMapNamingStrategy;
        this.integrationsConfigurationConfigMapNamingStrategy = integrationsConfigurationConfigMapNamingStrategy;
        this.sourceMountPointGetter = sourceMountPointGetter;
    }

    @Override
    public boolean enabled(ResourceBuildContext<List<Chain>> context) {
        return true;
    }

    @Override
    public String build(ResourceBuildContext<List<Chain>> context) throws Exception {
        if (context.getData().isEmpty()) {
            throw new CustomResourceBuildError("Chain list is empty");
        }

        TemplateData templateData = buildTemplateData(context);
        Context templateContext = Context.newContext(templateData);
        Template template = templates.compile(TEMPLATE_NAME);
        return template.apply(templateContext);
    }

    private TemplateData buildTemplateData(ResourceBuildContext<List<Chain>> context) {
        return TemplateData.builder()
                .name(integrationResourceNamingStrategy.getName(context))
                .container(buildContainerData(context.getBuildInfo().getOptions().getContainer()))
                .resources(buildResources(context))
                .propertiesEnabled(!context.getBuildInfo().getOptions()
                        .getIntegrations().isConfigurationConfigMapNeeded())
                .properties(buildCamelProperties(context))
                .environment(buildEnvironmentVars(context))
                .serviceAccountName(buildServiceAccountName(context))
                .build();
    }

    private ContainerData buildContainerData(ContainerOptions containerOptions) {
        String image = containerOptions.getImage();
        if (StringUtils.isBlank(image)) {
            image = "{{ .Values.container.image }}";
        }
        return ContainerData.builder()
                .image(image)
                .imagePullPolicy(containerOptions.getImagePoolPolicy().name())
                .build();
    }

    private String buildServiceAccountName(ResourceBuildContext<List<Chain>> context) {
        String serviceAccount = context.getBuildInfo().getOptions().getServiceAccount();
        return StringUtils.isBlank(serviceAccount)
                ? "{{ .Values.serviceAccountName }}"
                : serviceAccount;
    }

    private Collection<String> buildResources(ResourceBuildContext<List<Chain>> context) {
        List<String> resources = context.getData()
                .stream()
                .map(chain -> {
                    ResourceBuildContext<Chain> chainResourceBuildContext = context.updateTo(chain);
                    String name = configMapNamingStrategy.getName(chainResourceBuildContext);
                    return String.format("configmap:%s/%s@%s",
                            name, SourceConfigMapBuilder.CONTENT_KEY, sourceMountPointGetter.apply(chainResourceBuildContext));
                })
                .collect(Collectors.toList());
        if (context.getBuildInfo().getOptions().getIntegrations().isConfigurationConfigMapNeeded()) {
            String name = integrationsConfigurationConfigMapNamingStrategy.getName(context);
            String resource = String.format("configmap:%s/%s@%s", name,
                    IntegrationsConfigurationConfigMapBuilder.CONTENT_KEY, QIP_CHAINS_CONFIGURATION_PATH);
            resources.add(resource);
        }
        return resources;
    }

    private Collection<String> buildCamelProperties(ResourceBuildContext<List<Chain>> context) {
        List<Chain> chains = context.getData();
        ResourceBuildOptions options = context.getBuildInfo().getOptions();
        return IntStream.range(0, chains.size())
                .mapToObj(index -> {
                    Chain chain = chains.get(index);
                    String path = sourceMountPointGetter.apply(context.updateTo(chain));
                    return List.of(
                            String.format("camel.k.sources[%d].language = %s", index, options.getLanguage()),
                            String.format("camel.k.sources[%d].location = file:%s", index, path),
                            String.format("camel.k.sources[%d].name = %s", index, chain.getName()),
                            String.format("camel.k.sources[%d].id = %s", index, chain.getId())
                    );
                })
                .flatMap(Collection::stream)
                .toList();
    }

    private Collection<String> buildEnvironmentVars(ResourceBuildContext<List<Chain>> context) {
        Map<String, String> environment = new HashMap<>(DEFAULT_ENVIRONMENT);
        environment.putAll(context.getBuildInfo().getOptions().getEnvironment());
        environment.put("CLOUD_SERVICE_NAME", serviceNamingStrategy.getName(context));
        if (!context.getBuildInfo().getOptions().getIntegrations().isCamelKSourcesUtilized()) {
            String location = context.getBuildInfo().getOptions().getIntegrations().getConfigurationLocation();
            environment.put("QIP_CHAINS_CONFIGURATION_URL",
                    StringUtils.isBlank(location) ? "file:" + QIP_CHAINS_CONFIGURATION_PATH : location);
        }
        return environment
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toList();
    }
}
