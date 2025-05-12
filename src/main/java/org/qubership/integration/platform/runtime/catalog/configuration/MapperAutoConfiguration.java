/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.integration.platform.runtime.catalog.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.core.util.Json;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Dependency;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.MaskedField;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.runtime.catalog.service.codeview.deserializer.CodeviewChainElementDeserializer;
import org.qubership.integration.platform.runtime.catalog.service.codeview.serializer.CodeviewChainElementSerializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.deserializer.ChainDeserializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.deserializer.ChainElementDeserializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.entity.ChainDeserializationResult;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.entity.ElementDeserializationResult;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.ChainElementSerializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.ChainSerializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.DependencySerializer;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.MaskedFieldSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.yaml.snakeyaml.LoaderOptions;

@AutoConfiguration
public class MapperAutoConfiguration {
    private static final int CODE_POINT_LIMIT_MB = 256;

    @Primary
    @Bean(name = {"objectMapper", "jsonMapper"})
    @ConditionalOnProperty(prefix = "app", name = "prefix", havingValue = "qip")
    public ObjectMapper objectMapper() {
        return qipPrimaryObjectMapper();
    }

    @Bean("primaryObjectMapper")
    public ObjectMapper qipPrimaryObjectMapper() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));

        return objectMapper;
    }

    @Bean("yamlMapper")
    public YAMLMapper yamlMapper(ChainDeserializer chainDeserializer,
                                 ChainElementDeserializer chainElementDeserializer) {
        YAMLMapper yamlMapper = new YAMLMapper(createCustomYamlFactory());
        SimpleModule serializeModule = new SimpleModule();
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        serializeModule.addSerializer(Chain.class, new ChainSerializer());
        serializeModule.addSerializer(ChainElement.class, new ChainElementSerializer());
        serializeModule.addSerializer(Dependency.class, new DependencySerializer());
        serializeModule.addSerializer(MaskedField.class, new MaskedFieldSerializer());

        serializeModule.addDeserializer(ChainDeserializationResult.class, chainDeserializer);
        serializeModule.addDeserializer(ElementDeserializationResult.class, chainElementDeserializer);
        yamlMapper.registerModule(serializeModule);
        yamlMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));

        return yamlMapper;
    }

    @Bean("openApiObjectMapper")
    public ObjectMapper openApiObjectMapper() {
        return Json.mapper();
    }

    @Bean("defaultYamlMapper")
    public YAMLMapper defaultYamlMapper() {
        return new YAMLMapper(createCustomYamlFactory());
    }

    @Bean("yamlExportImportMapper")
    public YAMLMapper yamlExportImportMapper() {
        final String[] excludedFields = {
                "createdWhen",
                "createdBy",
                "modifiedBy",
                "classifier",
                "classifierV3",
                "status",
                "sourceHash"
        };

        YAMLMapper yamlMapper = new YAMLMapper(createCustomYamlFactory());

        SimpleModule serializeModule = new SimpleModule();

        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.registerModule(serializeModule);
        SimpleFilterProvider simpleFilterProvider = new SimpleFilterProvider().setFailOnUnknownId(false);
        simpleFilterProvider.addFilter("baseEntityFilter",
                SimpleBeanPropertyFilter.serializeAllExcept(excludedFields));
        yamlMapper.setFilterProvider(simpleFilterProvider);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return yamlMapper;
    }

    @Bean("codeViewYamlMapper")
    public YAMLMapper codeViewYamlMapper(@Qualifier("primaryObjectMapper") ObjectMapper objectMapper) {
        YAMLMapper yamlMapper = new YAMLMapper(createCustomYamlFactory());
        SimpleModule serializeModule = new SimpleModule();
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        serializeModule.addSerializer(ChainElement.class, new CodeviewChainElementSerializer());
        serializeModule.addDeserializer(ChainElement.class, new CodeviewChainElementDeserializer(objectMapper));
        yamlMapper.registerModule(serializeModule);

        return yamlMapper;
    }

    private YAMLFactory createCustomYamlFactory() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(CODE_POINT_LIMIT_MB * 1024 * 1024);
        return YAMLFactory.builder().loaderOptions(loaderOptions).build();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
            @Qualifier("primaryObjectMapper") ObjectMapper jsonMapper) {
        return new MappingJackson2HttpMessageConverter(jsonMapper);
    }
}
