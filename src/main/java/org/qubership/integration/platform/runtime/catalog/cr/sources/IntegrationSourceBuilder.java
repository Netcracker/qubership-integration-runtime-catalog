package org.qubership.integration.platform.runtime.catalog.cr.sources;

import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;

public interface IntegrationSourceBuilder {
    String getLanguageName();

    String build(Chain chain) throws Exception;
}
