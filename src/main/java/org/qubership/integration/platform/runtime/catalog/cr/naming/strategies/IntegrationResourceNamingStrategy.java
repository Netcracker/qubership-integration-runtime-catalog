package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.runtime.catalog.cr.BuildInfo;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IntegrationResourceNamingStrategy extends K8sResourceNamingStrategy<ResourceBuildContext<List<Chain>>> {
    @Override
    protected String proposeName(ResourceBuildContext<List<Chain>> context) {
        BuildInfo buildInfo = context.getBuildInfo();
        String name = buildInfo.getOptions().getName();
        return StringUtils.isNotBlank(name)
                ? name
                : "integration-" + buildInfo.getId();
    }
}
