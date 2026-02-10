package org.qubership.integration.platform.runtime.catalog.cr;

import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CustomResourceBuildService {
    private final List<ResourceBuilder<Chain>> chainResourceBuilders;
    private final List<ResourceBuilder<List<Chain>>> commonResourceBuilders;
    private final CustomResourceBuildContextFactory buildContextFactory;

    @Autowired
    public CustomResourceBuildService(
            List<ResourceBuilder<Chain>> chainResourceBuilders,
            List<ResourceBuilder<List<Chain>>> commonResourceBuilders,
            CustomResourceBuildContextFactory buildContextFactory
    ) {
        this.chainResourceBuilders = chainResourceBuilders;
        this.commonResourceBuilders = commonResourceBuilders;
        this.buildContextFactory = buildContextFactory;
    }

    public String buildResources(ResourceBuildRequest request) {
        return buildResources(request, false);
    }

    public String buildResources(ResourceBuildRequest request, boolean appendToExisting) {
        ResourceBuildContext<List<Chain>> buildContext =
                buildContextFactory.createResourceBuildContext(request, appendToExisting);
        return buildResources(buildContext);
    }

    public String buildResources(
            ResourceBuildContext<List<Chain>> buildContext
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (Chain chain : buildContext.getData()) {
                applyBuilders(stringBuilder, buildContext.updateTo(chain), chainResourceBuilders);
            }
            applyBuilders(stringBuilder, buildContext, commonResourceBuilders);
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
            if (builder.enabled(context)) {
                stringBuilder.append(builder.build(context));
            }
        }
    }


}
