package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapNamingStrategy implements NamingStrategy<Chain> {
    @Override
    public String getName(Chain context) {
        return "configmap-" + context.getId();
    }
}
