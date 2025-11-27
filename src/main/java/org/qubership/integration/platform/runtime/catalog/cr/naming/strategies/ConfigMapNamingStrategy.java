package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapNamingStrategy implements NamingStrategy<ResourceBuildContext<Chain>> {
    @Value("${app.prefix}")
    private String prefix;

    @Override
    public String getName(ResourceBuildContext<Chain> context) {
        Chain chain = context.getData();
        // We need to add a build ID as a suffix because there could be
        // several Camel K Integration resources that run the same chain.
        return String.format("%s-chain-%s-%s", prefix, chain.getId(), context.getBuildInfo().getId());
    }
}
