package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans;

import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;

public interface ChainBeanBuilder {
    void build(
            XMLStreamWriter2 streamWriter,
            Chain chain,
            SourceBuilderContext context
    ) throws Exception;
}
