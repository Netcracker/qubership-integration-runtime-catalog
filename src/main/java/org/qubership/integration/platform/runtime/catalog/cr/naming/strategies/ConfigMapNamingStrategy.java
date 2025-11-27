package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapNamingStrategy implements NamingStrategy<Chain> {
    @Value("${app.prefix}")
    private String prefix;

    @Override
    public String getName(Chain context) {
        return prefix + "-chain-" + context.getId();
    }
}
