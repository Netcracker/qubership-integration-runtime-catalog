package org.qubership.integration.platform.runtime.catalog.cr;

public interface ResourceBuilder<T> {
    String build(ResourceBuildContext<T> context) throws Exception;
}
