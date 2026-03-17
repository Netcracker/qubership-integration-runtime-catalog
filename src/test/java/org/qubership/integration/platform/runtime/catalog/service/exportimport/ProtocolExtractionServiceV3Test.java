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

package org.qubership.integration.platform.runtime.catalog.service.exportimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.integration.platform.runtime.catalog.model.system.OperationProtocol;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtocolExtractionServiceV3Test {

    private ProtocolExtractionService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        service = new ProtocolExtractionService(objectMapper, yamlMapper);
    }

    @Test
    void getProtocolFromV3YamlWithServers() throws IOException {
        byte[] content = readResourceBytes("asyncapi/v3/kafka-v3-simple.yaml");
        MockMultipartFile file = new MockMultipartFile("spec", "spec.yaml", null, content);
        Collection<MultipartFile> files = Collections.singletonList(file);

        OperationProtocol protocol = service.getOperationProtocol(files);

        assertNotNull(protocol);
        assertEquals(OperationProtocol.KAFKA, protocol);
    }

    @Test
    void getProtocolFromV3AmqpYaml() throws IOException {
        byte[] content = readResourceBytes("asyncapi/v3/amqp-v3-simple.yaml");
        MockMultipartFile file = new MockMultipartFile("spec", "spec.yaml", null, content);
        Collection<MultipartFile> files = Collections.singletonList(file);

        OperationProtocol protocol = service.getOperationProtocol(files);

        assertNotNull(protocol);
        assertEquals(OperationProtocol.AMQP, protocol);
    }

    @Test
    void getProtocolFromV3JsonWithXProtocol() throws IOException {
        byte[] content = readResourceBytes("asyncapi/v3/kafka-v3-no-servers.json");
        MockMultipartFile file = new MockMultipartFile("spec", "spec.json", null, content);
        Collection<MultipartFile> files = Collections.singletonList(file);

        OperationProtocol protocol = service.getOperationProtocol(files);

        assertNotNull(protocol);
        assertEquals(OperationProtocol.KAFKA, protocol);
    }

    @Test
    void getProtocolFromV2YamlStillWorks() {
        String v2Content = """
                asyncapi: 2.6.0
                info:
                  title: Test
                  version: 1.0.0
                servers:
                  production:
                    url: kafka.example.com:9092
                    protocol: kafka
                channels:
                  test/topic:
                    publish:
                      operationId: testOp
                """;
        MockMultipartFile file = new MockMultipartFile("spec", "spec.yaml", null,
                v2Content.getBytes(StandardCharsets.UTF_8));
        Collection<MultipartFile> files = Collections.singletonList(file);

        OperationProtocol protocol = service.getOperationProtocol(files);

        assertNotNull(protocol);
        assertEquals(OperationProtocol.KAFKA, protocol);
    }

    private byte[] readResourceBytes(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            return is.readAllBytes();
        }
    }
}
