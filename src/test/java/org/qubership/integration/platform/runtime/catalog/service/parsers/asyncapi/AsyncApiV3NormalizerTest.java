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

package org.qubership.integration.platform.runtime.catalog.service.parsers.asyncapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.AsyncapiSpecification;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.Channel;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.Message;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.OperationObject;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.components.Components;
import org.qubership.integration.platform.runtime.catalog.model.system.asyncapi.v3.AsyncapiV3Specification;
import org.qubership.integration.platform.runtime.catalog.service.resolvers.async.impl.AMQPSpecificationResolver;
import org.qubership.integration.platform.runtime.catalog.service.resolvers.async.impl.KafkaSpecificationResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AsyncApiV3NormalizerTest {

    private AsyncApiV3Normalizer normalizer;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        normalizer = new AsyncApiV3Normalizer(new ObjectMapper());
    }

    @Test
    void normalizeKafkaSimple() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-simple.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertEquals("2.6.0", v2.getAsyncapi());
        assertNotNull(v2.getInfo());
        assertEquals("Kafka Simple API", v2.getInfo().getTitle());
        assertEquals("1.0.0", v2.getInfo().getVersion());

        assertNotNull(v2.getServers());
        assertEquals(1, v2.getServers().size());
        assertEquals("kafka.example.com:9092", v2.getServers().get("production").getUrl());
        assertEquals("kafka", v2.getServers().get("production").getProtocol());

        assertNotNull(v2.getChannels());
        assertTrue(v2.getChannels().containsKey("user/signedup"));
        Channel channel = v2.getChannels().get("user/signedup");
        assertNotNull(channel.getPublish());
        assertEquals("publishUserSignedUp", channel.getPublish().getOperationId());
        assertNull(channel.getSubscribe());
    }

    @Test
    void normalizeMultiOperation() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-multi-operation.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertTrue(v2.getChannels().containsKey("user/events"));
        Channel channel = v2.getChannels().get("user/events");
        assertNotNull(channel.getPublish());
        assertNotNull(channel.getSubscribe());
        assertEquals("publishUserEvent", channel.getPublish().getOperationId());
        assertEquals("consumeUserEvent", channel.getSubscribe().getOperationId());
    }

    @Test
    void normalizeWithComponentRefs() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-with-refs.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertTrue(v2.getChannels().containsKey("order/created"));
        Channel channel = v2.getChannels().get("order/created");
        assertNotNull(channel.getPublish());
        assertNotNull(channel.getPublish().getMessage());

        assertNotNull(v2.getComponents());
        assertNotNull(v2.getComponents().getMessages());
        assertTrue(v2.getComponents().getMessages().containsKey("OrderCreated"));
        assertNotNull(v2.getComponents().getSchemas());
        assertTrue(v2.getComponents().getSchemas().containsKey("OrderId"));
    }

    @Test
    void normalizeRequestReply() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-request-reply.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertTrue(v2.getChannels().containsKey("order/request"));
        Channel requestChannel = v2.getChannels().get("order/request");
        assertNotNull(requestChannel.getPublish());
        assertEquals("sendOrderRequest", requestChannel.getPublish().getOperationId());

        assertTrue(v2.getChannels().containsKey("order/reply"));
        Channel replyChannel = v2.getChannels().get("order/reply");
        assertNotNull(replyChannel.getSubscribe());
        assertEquals("sendOrderRequestReply", replyChannel.getSubscribe().getOperationId());
        assertNotNull(replyChannel.getSubscribe().getMessage());
    }

    @Test
    void normalizeAmqpSimple() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/amqp-v3-simple.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertNotNull(v2.getServers());
        assertEquals("rabbitmq.example.com:5672/vhost",
                v2.getServers().get("production").getUrl());
        assertEquals("amqp", v2.getServers().get("production").getProtocol());

        assertTrue(v2.getChannels().containsKey("notifications"));
        Channel channel = v2.getChannels().get("notifications");
        assertNotNull(channel.getSubscribe());
        assertEquals("consumeNotification", channel.getSubscribe().getOperationId());
        assertNotNull(channel.getBindings());
        assertEquals("Notification queue", channel.getDescription());
    }

    @Test
    void normalizeWithNoServers() throws IOException {
        AsyncapiV3Specification v3 = readJsonV3("asyncapi/v3/kafka-v3-no-servers.json");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertNotNull(v2.getServers());
        assertTrue(v2.getServers().isEmpty());
        assertNotNull(v2.getInfo());
        assertEquals("kafka", v2.getInfo().getProtocol());

        assertTrue(v2.getChannels().containsKey("events/all"));
    }

    @Test
    void normalizeInlineChannelMessageIsResolved() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-simple.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        Channel channel = v2.getChannels().get("user/signedup");
        Message message = channel.getPublish().getMessage();
        assertNotNull(message);
        assertNull(message.get$ref(), "Inline channel message should be resolved, not left as $ref");
        assertNotNull(message.getPayload(), "Payload should be extracted from inline channel message");
        assertEquals("UserSignedUp", message.getName());
    }

    @Test
    void normalizeComponentRefIsPreserved() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-with-refs.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        Channel channel = v2.getChannels().get("order/created");
        Message message = channel.getPublish().getMessage();
        assertNotNull(message);
        assertEquals("#/components/messages/OrderCreated", message.get$ref(),
                "Component $ref should be preserved as-is");
    }

    @Test
    void normalizePreservesV3ActionOnOperations() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-multi-operation.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        Channel channel = v2.getChannels().get("user/events");
        assertNotNull(channel.getPublish());
        assertEquals("send", channel.getPublish().getAction());
        assertNotNull(channel.getSubscribe());
        assertEquals("receive", channel.getSubscribe().getAction());
    }

    @Test
    void kafkaResolverReturnsV3ActionWhenPresent() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-multi-operation.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);
        KafkaSpecificationResolver resolver = new KafkaSpecificationResolver(null);

        Channel channel = v2.getChannels().get("user/events");
        assertEquals("send", resolver.getMethod(channel, channel.getPublish()));
        assertEquals("receive", resolver.getMethod(channel, channel.getSubscribe()));
    }

    @Test
    void kafkaResolverFallsBackForV2Operations() {
        KafkaSpecificationResolver resolver = new KafkaSpecificationResolver(null);
        Channel channel = new Channel();
        OperationObject pub = new OperationObject();
        channel.setPublish(pub);

        assertEquals("publish", resolver.getMethod(channel, pub));

        OperationObject sub = new OperationObject();
        channel.setSubscribe(sub);
        assertEquals("subscribe", resolver.getMethod(channel, sub));
    }

    @Test
    void amqpResolverReturnsV3ActionWhenPresent() {
        AMQPSpecificationResolver resolver = new AMQPSpecificationResolver(null);
        Channel channel = new Channel();
        OperationObject op = new OperationObject();
        op.setAction("send");
        channel.setPublish(op);

        assertEquals("send", resolver.getMethod(channel, op));
    }

    @Test
    void amqpResolverFallsBackForV2Operations() {
        AMQPSpecificationResolver resolver = new AMQPSpecificationResolver(null);
        Channel channel = new Channel();
        OperationObject pub = new OperationObject();
        channel.setPublish(pub);

        assertEquals("subscribe", resolver.getMethod(channel, pub));

        OperationObject sub = new OperationObject();
        channel.setSubscribe(sub);
        assertEquals("publish", resolver.getMethod(channel, sub));
    }

    @Test
    void normalizeKafkaComplexSchemas() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/kafka-v3-complex-schemas.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertEquals(2, v2.getServers().size());
        assertNotNull(v2.getServers().get("production"));
        assertNotNull(v2.getServers().get("staging"));

        Map<String, Channel> channels = v2.getChannels();
        assertEquals(4, channels.size());
        assertTrue(channels.containsKey("orders.commands"));
        assertTrue(channels.containsKey("orders.events"));
        assertTrue(channels.containsKey("orders.cancellations"));
        assertTrue(channels.containsKey("inventory.stock.updates"));

        // createOrder → send → publish on orders.commands
        Channel commandsChannel = channels.get("orders.commands");
        assertNotNull(commandsChannel.getPublish());
        assertEquals("createOrder", commandsChannel.getPublish().getOperationId());
        Message createOrderMsg = commandsChannel.getPublish().getMessage();
        assertNotNull(createOrderMsg);
        assertEquals("#/components/messages/CreateOrder", createOrderMsg.get$ref());

        // onOrderCancelled → receive → subscribe on orders.cancellations (inline message)
        Channel cancellationsChannel = channels.get("orders.cancellations");
        assertNotNull(cancellationsChannel.getSubscribe());
        assertEquals("onOrderCancelled", cancellationsChannel.getSubscribe().getOperationId());
        Message cancelledMsg = cancellationsChannel.getSubscribe().getMessage();
        assertNotNull(cancelledMsg);
        assertNotNull(cancelledMsg.getPayload());
        assertNotNull(cancelledMsg.getPayload().getRequired());
        assertTrue(cancelledMsg.getPayload().getRequired().contains("orderId"));
        assertTrue(cancelledMsg.getPayload().getRequired().contains("reason"));
        assertNotNull(cancelledMsg.getPayload().getProperties().get("reason"),
                "reason property with enum should be preserved");

        // inventoryUpdates — inline message with additionalProperties on nested metadata
        Channel inventoryChannel = channels.get("inventory.stock.updates");
        assertNotNull(inventoryChannel.getSubscribe());
        Message stockMsg = inventoryChannel.getSubscribe().getMessage();
        assertNotNull(stockMsg);
        assertNotNull(stockMsg.getPayload());
        assertNotNull(stockMsg.getPayload().getRequired());
        assertTrue(stockMsg.getPayload().getRequired().contains("sku"));
        assertNotNull(stockMsg.getPayload().getProperties().get("metadata"),
                "metadata property with additionalProperties should be preserved");

        // components: schemas with nested refs, required, constraints
        Components components = v2.getComponents();
        assertNotNull(components);
        assertNotNull(components.getMessages());
        assertTrue(components.getMessages().containsKey("CreateOrder"));
        assertTrue(components.getMessages().containsKey("OrderCreated"));
        assertNotNull(components.getSchemas());
        assertTrue(components.getSchemas().containsKey("OrderItem"));
        assertTrue(components.getSchemas().containsKey("Address"));
        assertTrue(components.getSchemas().containsKey("PercentageDiscount"));
        assertTrue(components.getSchemas().containsKey("FixedDiscount"));

        // Verify schema constraints are preserved
        var orderItemSchema = components.getSchemas().get("OrderItem");
        assertNotNull(orderItemSchema.getRequired());
        assertEquals(3, orderItemSchema.getRequired().size());
        assertTrue(orderItemSchema.getRequired().contains("sku"));
        assertTrue(orderItemSchema.getRequired().contains("quantity"));
        assertTrue(orderItemSchema.getRequired().contains("unitPrice"));
        assertNotNull(orderItemSchema.getProperties());
        assertTrue(orderItemSchema.getProperties().containsKey("sku"));
        assertTrue(orderItemSchema.getProperties().containsKey("quantity"));

        var addressSchema = components.getSchemas().get("Address");
        assertNotNull(addressSchema.getRequired());
        assertTrue(addressSchema.getRequired().contains("street"));
        assertTrue(addressSchema.getRequired().contains("city"));
        assertTrue(addressSchema.getRequired().contains("country"));
    }

    @Test
    void normalizeAmqpExchangeBindings() throws IOException {
        AsyncapiV3Specification v3 = readYamlV3("asyncapi/v3/amqp-v3-exchange-bindings.yaml");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertEquals(2, v2.getServers().size());
        assertEquals("rabbitmq.prod.internal:5672/notifications",
                v2.getServers().get("production").getUrl());
        assertEquals("rabbitmq-failover.prod.internal:5672/notifications",
                v2.getServers().get("failover").getUrl());

        Map<String, Channel> channels = v2.getChannels();
        assertEquals(3, channels.size());
        assertTrue(channels.containsKey("notifications.email"));
        assertTrue(channels.containsKey("notifications.sms"));
        assertTrue(channels.containsKey("notifications.dlq"));

        // Email queue bindings preserved
        Channel emailChannel = channels.get("notifications.email");
        assertNotNull(emailChannel.getBindings());
        @SuppressWarnings("unchecked")
        Map<String, Object> amqpBindings = (Map<String, Object>) emailChannel.getBindings().get("amqp");
        assertNotNull(amqpBindings);
        assertEquals("queue", amqpBindings.get("is"));
        @SuppressWarnings("unchecked")
        Map<String, Object> queueBinding = (Map<String, Object>) amqpBindings.get("queue");
        assertEquals("notifications.email", queueBinding.get("name"));
        assertEquals(true, queueBinding.get("durable"));
        @SuppressWarnings("unchecked")
        Map<String, Object> exchangeBinding = (Map<String, Object>) amqpBindings.get("exchange");
        assertEquals("notifications.topic", exchangeBinding.get("name"));
        assertEquals("topic", exchangeBinding.get("type"));

        // sendEmail → publish, consumeEmail → subscribe on same channel
        assertNotNull(emailChannel.getPublish());
        assertNotNull(emailChannel.getSubscribe());
        assertEquals("sendEmail", emailChannel.getPublish().getOperationId());
        assertEquals("consumeEmail", emailChannel.getSubscribe().getOperationId());

        // Email message — inline with complex nested structure
        Message emailMsg = emailChannel.getSubscribe().getMessage();
        assertNotNull(emailMsg);
        assertNotNull(emailMsg.getPayload());
        assertNotNull(emailMsg.getPayload().getRequired());
        assertTrue(emailMsg.getPayload().getRequired().contains("to"));
        assertTrue(emailMsg.getPayload().getRequired().contains("subject"));
        assertTrue(emailMsg.getPayload().getRequired().contains("templateId"));

        // DLQ channel bindings — fanout exchange
        Channel dlqChannel = channels.get("notifications.dlq");
        assertNotNull(dlqChannel.getBindings());
        assertNotNull(dlqChannel.getSubscribe());
        assertEquals("processDeadLetters", dlqChannel.getSubscribe().getOperationId());
    }

    @Test
    void normalizeEmptyOperations() {
        AsyncapiV3Specification v3 = new AsyncapiV3Specification();
        v3.setAsyncapi("3.0.0");
        AsyncapiSpecification v2 = normalizer.normalize(v3);

        assertEquals("2.6.0", v2.getAsyncapi());
        assertNotNull(v2.getChannels());
        assertTrue(v2.getChannels().isEmpty());
    }

    private AsyncapiV3Specification readYamlV3(String path) throws IOException {
        String content = readResource(path);
        return yamlMapper.readValue(content, AsyncapiV3Specification.class);
    }

    private AsyncapiV3Specification readJsonV3(String path) throws IOException {
        String content = readResource(path);
        return jsonMapper.readValue(content, AsyncapiV3Specification.class);
    }

    private String readResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
