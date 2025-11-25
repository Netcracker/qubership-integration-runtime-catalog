package org.qubership.integration.platform.runtime.catalog.cr;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ResourceBuilder<T> {
    ObjectNode build(T entity, ResourceBuildContext context) throws Exception;
}
