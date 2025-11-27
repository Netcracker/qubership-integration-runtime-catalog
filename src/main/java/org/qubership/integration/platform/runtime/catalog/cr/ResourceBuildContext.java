package org.qubership.integration.platform.runtime.catalog.cr;

import lombok.Getter;

@Getter
public class ResourceBuildContext<T> {
    private BuildInfo buildInfo;
    private T data;

    public static <T> ResourceBuildContext<T> create(BuildInfo buildInfo, T data) {
        return new ResourceBuildContext<T>(buildInfo, data);
    }

    private ResourceBuildContext(BuildInfo buildInfo, T data) {
        this.buildInfo = buildInfo;
        this.data = data;
    }
}
