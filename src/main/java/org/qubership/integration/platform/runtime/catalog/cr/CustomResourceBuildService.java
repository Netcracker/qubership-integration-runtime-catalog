package org.qubership.integration.platform.runtime.catalog.cr;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.cr.naming.strategies.BuildNamingContext;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CustomResourceBuildService {
    private final YAMLMapper yamlMapper;
    private final NamingStrategy<BuildNamingContext> buildNamingStrategy;
    private final List<ResourceBuilder<Chain>> chainResourceBuilders;
    private final List<ResourceBuilder<List<Chain>>> commonResourceBuilders;

    @Autowired
    public CustomResourceBuildService(
            @Qualifier("customResourceYamlMapper") YAMLMapper yamlMapper,
            NamingStrategy<BuildNamingContext> buildNamingStrategy,
            List<ResourceBuilder<Chain>> chainResourceBuilders,
            List<ResourceBuilder<List<Chain>>> commonResourceBuilders
    ) {
        this.yamlMapper = yamlMapper;
        this.buildNamingStrategy = buildNamingStrategy;
        this.chainResourceBuilders = chainResourceBuilders;
        this.commonResourceBuilders = commonResourceBuilders;
    }

    public String buildCustomResource(
            List<Chain> chains,
            ResourceBuildOptions options
    ) {
        BuildInfo buildInfo = createBuildInfo(options);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (SequenceWriter sequenceWriter = yamlMapper.writer().writeValues(outputStream)) {
            for (Chain chain : chains) {
                applyBuilders(sequenceWriter, ResourceBuildContext.create(buildInfo, chain), chainResourceBuilders);
            }
            applyBuilders(sequenceWriter, ResourceBuildContext.create(buildInfo, chains), commonResourceBuilders);
            outputStream.flush();
            return outputStream.toString();
        } catch (Exception e) {
            log.error("Failed to build custom resource", e);
            throw new CustomResourceBuildError("Failed to build custom resource", e);
        }
    }

    private static <T> void applyBuilders(
            SequenceWriter sequenceWriter,
            ResourceBuildContext<T> context,
            List<ResourceBuilder<T>> builders
    ) throws Exception {
        for (var builder : builders) {
            sequenceWriter.write(builder.build(context));
        }
    }

    private BuildInfo createBuildInfo(ResourceBuildOptions options) {
        String id = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        BuildNamingContext buildNamingContext = BuildNamingContext.builder()
                .id(id)
                .timestamp(timestamp)
                .build();
        return BuildInfo.builder()
                .id(id)
                .timestamp(timestamp)
                .name(buildNamingStrategy.getName(buildNamingContext))
                .options(options)
                .build();
    }
}
