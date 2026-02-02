package org.qubership.integration.platform.runtime.catalog.cr.locations;

import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.function.Function;

@Component
public class SourceMountPointGetter implements Function<ResourceBuildContext<Chain>, String> {
    // TODO make an application property
    private static final String MOUNT_DIR = "/etc/camel/sources/";

    private final NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy;

    @Autowired
    public SourceMountPointGetter(NamingStrategy<ResourceBuildContext<Chain>> configMapNamingStrategy) {
        this.configMapNamingStrategy = configMapNamingStrategy;
    }

    @Override
    public String apply(ResourceBuildContext<Chain> context) {
        String name = configMapNamingStrategy.getName(context);
        String fileName = String.format("%s.%s", name, context.getBuildInfo().getOptions().getLanguage());
        return Paths.get(MOUNT_DIR, fileName).toString();
    }
}
