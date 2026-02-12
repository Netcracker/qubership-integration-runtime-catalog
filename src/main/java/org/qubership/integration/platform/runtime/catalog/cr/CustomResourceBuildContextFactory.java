package org.qubership.integration.platform.runtime.catalog.cr;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.qubership.integration.platform.runtime.catalog.cr.integrations.configuration.IntegrationConfigurationSerdes;
import org.qubership.integration.platform.runtime.catalog.cr.integrations.configuration.IntegrationsConfiguration;
import org.qubership.integration.platform.runtime.catalog.cr.k8s.CamelKIntegration;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.naming.strategies.BuildNamingContext;
import org.qubership.integration.platform.runtime.catalog.cr.naming.strategies.SourceDslConfigMapNamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.kubernetes.KubeUtil;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.repository.chain.ChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static java.util.Objects.isNull;
import static org.qubership.integration.platform.runtime.catalog.kubernetes.KubeUtil.getName;

@Component
public class CustomResourceBuildContextFactory {
    private final ChainRepository chainRepository;
    private final NamingStrategy<BuildNamingContext> buildNamingStrategy;
    private final CustomResourceService customResourceService;
    private final IntegrationConfigurationSerdes integrationConfigurationSerdes;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;
    private final SourceDslConfigMapNamingStrategy sourceDslConfigMapNamingStrategy;

    @Autowired
    public CustomResourceBuildContextFactory(
            ChainRepository chainRepository,
            NamingStrategy<BuildNamingContext> buildNamingStrategy,
            CustomResourceService customResourceService,
            IntegrationConfigurationSerdes integrationConfigurationSerdes,
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy,
            @Qualifier("sourceDslConfigMapNamingStrategy") SourceDslConfigMapNamingStrategy sourceDslConfigMapNamingStrategy
    ) {
        this.chainRepository = chainRepository;
        this.buildNamingStrategy = buildNamingStrategy;
        this.customResourceService = customResourceService;
        this.integrationConfigurationSerdes = integrationConfigurationSerdes;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.sourceDslConfigMapNamingStrategy = sourceDslConfigMapNamingStrategy;
    }

    public ResourceBuildContext<List<Chain>> createResourceBuildContext(
            ResourceBuildRequest request,
            boolean appendToExising
    ) {
        List<Chain> chains = chainRepository.findAllByIdIn(request.getChainIds());

        ResourceBuildOptions options = appendToExising
                ? request.getOptions().toBuilder()
                        .monitoring(request.getOptions().getMonitoring().toBuilder()
                                .enabled(false)
                                .build())
                        .service(request.getOptions().getService().toBuilder()
                                .enabled(false)
                                .build())
                        .build()
                : request.getOptions().toBuilder().build();
        BuildInfo buildInfo = createBuildInfo(options);
        ResourceBuildContext<List<Chain>> context = ResourceBuildContext.create(buildInfo)
                .updateTo(chains);

        if (appendToExising) {
            addAppendConfigurationToContext(context);
        }

        return context;
    }

    private BuildInfo createBuildInfo(ResourceBuildOptions options) {
        String id = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        BuildNamingContext buildNamingContext = BuildNamingContext.builder()
                .id(id)
                .timestamp(timestamp)
                .build();
        return BuildInfo.builder()
                .id(id)
                .timestamp(timestamp)
                .name(buildNamingStrategy.getName(buildNamingContext))
                .options(options)
                .build();
    }

    private void addAppendConfigurationToContext(ResourceBuildContext<List<Chain>> context) {
        customResourceService
                .getIntegrationResources(integrationResourceNamingStrategy.getName(context))
                .ifPresent(resources -> {
                    updateIntegrationResources(context, resources.integration());
                    putIntegrationsConfigurationToBuildCache(context, resources.integrationsConfiguration());
                    putSourceConfigMapNamesToBuildCache(context, Optional.ofNullable(resources.integrationSources())
                            .orElse(Collections.emptyMap()));
                });
    }

    private void putIntegrationsConfigurationToBuildCache(
            ResourceBuildContext<List<Chain>> context,
            V1ConfigMap configMap
    ) {
        if (isNull(configMap)) {
            return;
        }
        IntegrationsConfiguration cfg = integrationConfigurationSerdes.getFromConfigMap(configMap);
        String key = getName(configMap).orElse(null);
        context.getBuildCache().put(key, cfg);
    }

    private void putSourceConfigMapNamesToBuildCache(
            ResourceBuildContext<List<Chain>> context,
            Map<String, V1ConfigMap> sourceConfigMaps
    ) {
        context.getData().forEach(chain ->
                Optional.ofNullable(sourceConfigMaps.get(chain.getId()))
                        .flatMap(KubeUtil::getName)
                        .ifPresent(name ->
                                sourceDslConfigMapNamingStrategy.useName(context.updateTo(chain), name)));
    }

    private void updateIntegrationResources(
            ResourceBuildContext<List<Chain>> context,
            CamelKIntegration integration
    ) {
        ResourceBuildOptions options = context.getBuildInfo().getOptions();
        Set<String> resources = new HashSet<>(integration.getSpec().getTraits().getMount().getResources());
        resources.addAll(options.getResources());
        options.setResources(resources);
    }
}
