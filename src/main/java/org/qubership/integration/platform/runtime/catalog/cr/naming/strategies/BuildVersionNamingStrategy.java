package org.qubership.integration.platform.runtime.catalog.cr.naming.strategies;

import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.CustomResourceOptions;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BuildVersionNamingStrategy implements NamingStrategy<CustomResourceOptions> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("'build-'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public String getName(CustomResourceOptions context) {
        return DATE_TIME_FORMATTER.format(ZonedDateTime.now());
    }
}
