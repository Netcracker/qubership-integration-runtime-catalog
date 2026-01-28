package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("serviceMonitorNamingStrategy")
public class ServiceMonitorNamingStrategy extends K8sResourceNamingStrategy<ResourceBuildContext<List<Chain>>> {
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;

    @Value("${qip.cr.naming.service-monitor.suffix:-monitor}")
    private String suffix;

    @Autowired
    public ServiceMonitorNamingStrategy(
            NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy
    ) {
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
    }

    @Override
    protected String proposeName(ResourceBuildContext<List<Chain>> context) {
        String name = integrationResourceNamingStrategy.getName(context) + suffix;
        return name.substring(0, Math.min(name.length(), K8S_RESOURCE_NAME_LENGTH_LIMIT));
    }
}
