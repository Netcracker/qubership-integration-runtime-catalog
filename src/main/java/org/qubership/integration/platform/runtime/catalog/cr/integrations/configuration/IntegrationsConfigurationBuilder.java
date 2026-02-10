package org.qubership.integration.platform.runtime.catalog.cr.integrations.configuration;

import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.locations.LibraryLocationGetterProvider;
import org.qubership.integration.platform.runtime.catalog.cr.locations.SourceLocationGetterProvider;
import org.qubership.integration.platform.runtime.catalog.model.constant.CamelOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class IntegrationsConfigurationBuilder {
    private final SourceLocationGetterProvider sourceLocationGetterProvider;
    private final LibraryLocationGetterProvider libraryLocationGetterProvider;

    @Autowired
    public IntegrationsConfigurationBuilder(
            SourceLocationGetterProvider sourceLocationGetterProvider,
            LibraryLocationGetterProvider libraryLocationGetterProvider
    ) {
        this.sourceLocationGetterProvider = sourceLocationGetterProvider;
        this.libraryLocationGetterProvider = libraryLocationGetterProvider;
    }

    public IntegrationsConfiguration build(ResourceBuildContext<List<Chain>> context) {
        List<Chain> chains = context.getData();
        return IntegrationsConfiguration.builder()
                .chains(chains.stream().map(chain -> buildSourceDefinition(context.updateTo(chain))).toList())
                .libraries(buildLibrariesDefinitions(context))
                .build();
    }

    private SourceDefinition buildSourceDefinition(ResourceBuildContext<Chain> context) {
        Chain chain = context.getData();
        return SourceDefinition.builder()
                .id(chain.getId())
                .name(chain.getName())
                .location(getChainSourceLocation(context))
                .language(context.getBuildInfo().getOptions().getLanguage())
                .build();
    }

    private List<LibraryDefinition> buildLibrariesDefinitions(
            ResourceBuildContext<List<Chain>> context
    ) {
        List<Chain> chains = context.getData();

        // TODO use findAll(specification) method of ElementRepository to get all specification IDs
        Stream<String> specificationIds = chains.stream()
                .map(Chain::getElements)
                .flatMap(Collection::stream)
                .map(ChainElement::getProperties)
                .map(properties -> properties.get(CamelOptions.SPECIFICATION_ID))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .distinct();

        return specificationIds
                .map(id -> LibraryDefinition.builder()
                        .specificationId(id)
                        .location(getLibraryLocation(id, context))
                        .build())
                .toList();
    }

    private String getChainSourceLocation(ResourceBuildContext<Chain> context) {
        return sourceLocationGetterProvider.get(context).apply(context);
    }

    private String getLibraryLocation(String id, ResourceBuildContext<List<Chain>> context) {
        return libraryLocationGetterProvider.get(context).apply(context.updateTo(id));
    }
}
