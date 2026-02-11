package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component("chainDslConfigMapNamingStrategy")
public class ChainDslConfigMapNamingStrategy extends K8sResourceNamingStrategy<ResourceBuildContext<Chain>> {
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;

    @Autowired
    public ChainDslConfigMapNamingStrategy(
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy
    ) {
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
    }

    @Override
    protected String proposeName(ResourceBuildContext<Chain> context) {
        Chain chain = context.getData();
        return context.getBuildCache()
                .computeIfAbsent(getKey(chain), k -> createUniqueName(context))
                .toString();
    }

    private String getKey(Chain chain) {
        return this.getClass().getSimpleName() + "-" + chain.getId();
    }

    private String createUniqueName(ResourceBuildContext<Chain> context) {
        String prefix = integrationResourceNamingStrategy.getName(context.updateTo(Collections.emptyList()));
        return String.format("%s-%s", prefix, UUID.randomUUID());
    }

    public void useName(ResourceBuildContext<Chain> context, String name) {
        Chain chain = context.getData();
        context.getBuildCache().put(getKey(chain), name);
    }
}
