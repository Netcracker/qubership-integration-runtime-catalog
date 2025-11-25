package org.qubership.integration.platform.runtime.catalog.cr;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.CustomResourceOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Service
public class CustomResourceBuildService {
    private final YAMLMapper yamlMapper;
    private final NamingStrategy<CustomResourceOptions> buildVersionNamingStrategy;
    private final List<ResourceBuilder<Chain>> chainResourceBuilders;
    private final List<ResourceBuilder<List<Chain>>> commonResourceBuilders;

    @Autowired
    public CustomResourceBuildService(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            NamingStrategy<CustomResourceOptions> buildVersionNamingStrategy,
            List<ResourceBuilder<Chain>> chainResourceBuilders,
            List<ResourceBuilder<List<Chain>>> commonResourceBuilders
    ) {
        this.yamlMapper = yamlMapper;
        this.buildVersionNamingStrategy = buildVersionNamingStrategy;
        this.chainResourceBuilders = chainResourceBuilders;
        this.commonResourceBuilders = commonResourceBuilders;
    }

    public String buildCustomResource(
            List<Chain> chains,
            CustomResourceOptions options
    ) {
        ResourceBuildContext buildContext = ResourceBuildContext.builder()
                .buildVersion(buildVersionNamingStrategy.getName(options))
                .options(options)
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (SequenceWriter sequenceWriter = yamlMapper.writer().writeValues(outputStream)) {
            for (Chain chain : chains) {
                applyBuilders(sequenceWriter, chain, buildContext, chainResourceBuilders);
            }
            applyBuilders(sequenceWriter, chains, buildContext, commonResourceBuilders);
            outputStream.flush();
            return outputStream.toString();
        } catch (Exception e) {
            log.error("Failed to build custom resource", e);
            throw new CustomResourceBuildError("Failed to build custom resource", e);
        }
    }

    private static <T> void applyBuilders(
            SequenceWriter sequenceWriter,
            T entity,
            ResourceBuildContext context,
            List<ResourceBuilder<T>> builders
    ) throws Exception {
        for (var builder : builders) {
            sequenceWriter.write(builder.build(entity, context));
        }
    }
}
