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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kubernetes.client.openapi.models.V1Secret;
import io.swagger.v3.core.util.Json;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.runtime.catalog.service.codeview.deserializer.CodeviewChainElementDeserializer;
import org.qubership.integration.platform.runtime.catalog.service.codeview.serializer.CodeviewChainElementSerializer;
import org.qubership.integration.platform.runtime.catalog.service.variables.secrets.KubeSecretSerializer;
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
    public YAMLMapper yamlMapper() {
        YAMLMapper yamlMapper = new YAMLMapper(createCustomYamlFactory());
        SimpleModule serializeModule = new SimpleModule();
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.registerModule(serializeModule);
        yamlMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return yamlMapper;
    }

    @Bean("customResourceYamlMapper")
    public YAMLMapper customResourceYamlMapper() {
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.SPLIT_LINES)
                .build();
        return new YAMLMapper(yamlFactory);
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

    @Bean("objectMapperWithSorting")
    public ObjectMapper objectMapperWithSorting() {
        ObjectMapper objectMapper = qipPrimaryObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        return objectMapper;
    }

    @Bean("variablesYamlMapper")
    public YAMLMapper variablesYamlMapper(KubeSecretSerializer kubeSecretSerializer) {
        YAMLMapper yamlMapper = new YAMLMapper();
        SimpleModule serializeModule = new SimpleModule();
        serializeModule.addSerializer(V1Secret.class, kubeSecretSerializer);

        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.registerModule(serializeModule);
        yamlMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        yamlMapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        yamlMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        return yamlMapper;
    }

    @Bean("variablesYamlImportExportMapper")
    public YAMLMapper variablesYamlImportExportMapper() {
        final String[] excludedFields = {"createdWhen", "modifiedWhen", "createdBy", "modifiedBy"};

        YAMLMapper yamlMapper = new YAMLMapper();
        SimpleModule serializeModule = new SimpleModule();

        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.registerModule(serializeModule);
        SimpleFilterProvider simpleFilterProvider = new SimpleFilterProvider().setFailOnUnknownId(false);
        simpleFilterProvider.addFilter("commonVariableFilter",
                SimpleBeanPropertyFilter.serializeAllExcept(excludedFields));
        yamlMapper.setFilterProvider(simpleFilterProvider);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
