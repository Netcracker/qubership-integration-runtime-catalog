package org.qubership.integration.platform.runtime.catalog.cr;

import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.naming.strategies.BuildNamingContext;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CustomResourceBuildService {
    private final NamingStrategy<BuildNamingContext> buildNamingStrategy;
    private final List<ResourceBuilder<Chain>> chainResourceBuilders;
    private final List<ResourceBuilder<List<Chain>>> commonResourceBuilders;

    @Autowired
    public CustomResourceBuildService(
            NamingStrategy<BuildNamingContext> buildNamingStrategy,
            List<ResourceBuilder<Chain>> chainResourceBuilders,
            List<ResourceBuilder<List<Chain>>> commonResourceBuilders
    ) {
        this.buildNamingStrategy = buildNamingStrategy;
        this.chainResourceBuilders = chainResourceBuilders;
        this.commonResourceBuilders = commonResourceBuilders;
    }

    public String buildCustomResource(
            List<Chain> chains,
            ResourceBuildOptions options
    ) {
        BuildInfo buildInfo = createBuildInfo(options);
        ResourceBuildContext<Void> buildContext = ResourceBuildContext.create(buildInfo);
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (Chain chain : chains) {
                applyBuilders(stringBuilder, buildContext.updateTo(chain), chainResourceBuilders);
            }
            applyBuilders(stringBuilder, buildContext.updateTo(chains), commonResourceBuilders);
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("Failed to build custom resource", e);
            throw new CustomResourceBuildError("Failed to build custom resource", e);
        }
    }

    private static <T> void applyBuilders(
            StringBuilder stringBuilder,
            ResourceBuildContext<T> context,
            List<ResourceBuilder<T>> builders
    ) throws Exception {
        for (var builder : builders) {
            stringBuilder.append(builder.build(context));
        }
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
}
