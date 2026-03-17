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

package org.qubership.integration.platform.runtime.catalog.service.resolvers.async.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.AsyncapiSpecification;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.Channel;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.OperationObject;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.Operation;
import org.qubership.integration.platform.runtime.catalog.service.resolvers.async.AsyncApiSchemaResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AMQPSpecificationResolverTest {

    private AMQPSpecificationResolver resolver;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    void setUp() {
        resolver = new AMQPSpecificationResolver(new AsyncApiSchemaResolver());
    }

    @Test
    void setUpOperationMessagesWithInlinePayload() throws IOException {
        AsyncapiSpecification spec = readYaml("asyncapi/v2/amqp-v2-with-messages.yaml");
        Channel channel = spec.getChannels().get("notifications");

        List<OperationObject> ops = resolver.getOperationObjects(channel);
        assertEquals(2, ops.size(), "Should return both publish and subscribe operations");

        // subscribe operation has inline payload
        OperationObject subscribeOp = channel.getSubscribe();
        Operation operation = Operation.builder().build();
        resolver.setUpOperationMessages(operation, subscribeOp, spec.getComponents());

        assertNotNull(operation.getResponseSchemas());
        assertEquals(1, operation.getResponseSchemas().size());
        assertTrue(operation.getResponseSchemas().containsKey("payload"));

        JsonNode payload = operation.getResponseSchemas().get("payload");
        assertEquals("object", payload.get("type").asText());
        assertTrue(payload.get("properties").has("to"));
        assertTrue(payload.get("properties").has("subject"));

        // full schema preserved: required fields must be present
        assertTrue(payload.has("required"));
        assertEquals(2, payload.get("required").size());
    }

    @Test
    void setUpOperationMessagesWithRef() throws IOException {
        AsyncapiSpecification spec = readYaml("asyncapi/v2/amqp-v2-with-messages.yaml");
        Channel channel = spec.getChannels().get("notifications");

        // publish operation has $ref
        OperationObject publishOp = channel.getPublish();
        Operation operation = Operation.builder().build();
        resolver.setUpOperationMessages(operation, publishOp, spec.getComponents());

        assertNotNull(operation.getResponseSchemas());
        assertEquals(1, operation.getResponseSchemas().size());
        assertTrue(operation.getResponseSchemas().containsKey("Notification"));
    }

    @Test
    void setUpOperationMessagesWithRefOneOf() throws IOException {
        AsyncapiSpecification spec = readYaml("asyncapi/v2/amqp-v2-with-messages.yaml");
        Channel channel = spec.getChannels().get("alerts");

        List<OperationObject> ops = resolver.getOperationObjects(channel);
        assertEquals(1, ops.size(), "Only subscribe operation on alerts channel");

        OperationObject subscribeOp = channel.getSubscribe();
        Operation operation = Operation.builder().build();
        resolver.setUpOperationMessages(operation, subscribeOp, spec.getComponents());

        assertNotNull(operation.getResponseSchemas());
        assertEquals(2, operation.getResponseSchemas().size());
        assertTrue(operation.getResponseSchemas().containsKey("CriticalAlert"));
        assertTrue(operation.getResponseSchemas().containsKey("WarningAlert"));
    }

    @Test
    void setUpOperationMessagesWithPayloadRef() throws IOException {
        AsyncapiSpecification spec = readYaml("asyncapi/v2/amqp-v2-payload-ref.yaml");
        Channel channel = spec.getChannels().get("events");

        OperationObject subscribeOp = channel.getSubscribe();
        Operation operation = Operation.builder().build();
        resolver.setUpOperationMessages(operation, subscribeOp, spec.getComponents());

        assertNotNull(operation.getResponseSchemas());
        assertEquals(1, operation.getResponseSchemas().size());
        assertTrue(operation.getResponseSchemas().containsKey("Event"));

        JsonNode schema = operation.getResponseSchemas().get("Event");
        // The resolved schema must contain the actual type and properties from the component schema
        assertEquals("object", schema.get("type").asText());
        assertTrue(schema.has("properties"));
        assertTrue(schema.get("properties").has("id"));
        assertTrue(schema.get("properties").has("type"));
        assertTrue(schema.get("properties").has("data"));

        // Nested $ref to EventData must be resolved in definitions
        assertTrue(schema.has("definitions"));
        assertTrue(schema.get("definitions").has("EventData"));
        JsonNode eventDataDef = schema.get("definitions").get("EventData");
        assertEquals("object", eventDataDef.get("type").asText());
        assertTrue(eventDataDef.get("properties").has("key"));
    }

    @Test
    void getOperationObjectsFallsBackToDummy() {
        Channel emptyChannel = new Channel();
        List<OperationObject> ops = resolver.getOperationObjects(emptyChannel);

        assertEquals(1, ops.size());
        assertEquals("AMQP operation", ops.get(0).getSummary());
    }

    private AsyncapiSpecification readYaml(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return yamlMapper.readValue(content, AsyncapiSpecification.class);
        }
    }
}
