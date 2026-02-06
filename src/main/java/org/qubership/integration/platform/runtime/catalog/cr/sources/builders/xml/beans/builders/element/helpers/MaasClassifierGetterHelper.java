package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.element.helpers;

import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.runtime.catalog.service.EnvironmentService;
import org.qubership.integration.platform.runtime.catalog.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelNames.MAAS_CLASSIFIER_NAME_PROP;
import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelNames.OPERATION_ASYNC_PROPERTIES;
import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelOptions.SYSTEM_ID;

@Component
public class MaasClassifierGetterHelper {
    private final SystemService systemService;
    private final EnvironmentService environmentService;

    @Autowired
    public MaasClassifierGetterHelper(
            SystemService systemService,
            EnvironmentService environmentService
    ) {
        this.systemService = systemService;
        this.environmentService = environmentService;
    }

    public String getMaasClassifierForServiceCallOrAsyncApiElement(ChainElement element) {
        return Optional.ofNullable(element.getPropertyAsString(OPERATION_ASYNC_PROPERTIES))
                .map(Map.class::cast)
                .map(m -> m.get(MAAS_CLASSIFIER_NAME_PROP))
                .or(() -> Optional.ofNullable(element.getPropertyAsString(SYSTEM_ID))
                        .map(systemService::getByIdOrNull)
                        .map(system -> environmentService.getByIdForSystem(
                                system.getId(), system.getActiveEnvironmentId()))
                        .map(env -> env.getProperties().path(MAAS_CLASSIFIER_NAME_PROP).asText()))
                .map(Object::toString)
                .orElse("");
    }
}
