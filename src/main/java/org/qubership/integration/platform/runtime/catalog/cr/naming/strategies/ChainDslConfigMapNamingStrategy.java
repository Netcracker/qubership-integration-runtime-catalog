package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("chainDslConfigMapNamingStrategy")
public class ChainDslConfigMapNamingStrategy extends K8sResourceNamingStrategy<ResourceBuildContext<Chain>> {
    @Value("${app.prefix}")
    private String prefix;

    @Override
    protected String proposeName(ResourceBuildContext<Chain> context) {
        Chain chain = context.getData();
        return context.getBuildCache()
                .computeIfAbsent(getKey(chain), k -> createUniqueName())
                .toString();
    }

    private String getKey(Chain chain) {
        return this.getClass().getSimpleName() + "-" + chain.getId();
    }

    private String createUniqueName() {
        return String.format("%s-chain-%s", prefix, UUID.randomUUID());
    }

    public void useName(ResourceBuildContext<Chain> context, String name) {
        Chain chain = context.getData();
        context.getBuildCache().put(getKey(chain), name);
    }
}
