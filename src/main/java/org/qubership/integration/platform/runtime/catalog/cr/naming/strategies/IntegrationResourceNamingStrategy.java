package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationResourceNamingStrategy implements NamingStrategy<Void> {
    @Override
    public String getName(Void context) {
        return "integration-" + UUID.randomUUID();
    }
}
