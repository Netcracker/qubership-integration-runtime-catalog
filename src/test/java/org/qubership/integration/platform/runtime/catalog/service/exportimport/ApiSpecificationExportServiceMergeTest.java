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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ApiSpecificationExportServiceMergeTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ApiSpecificationExportService service;
    private Method mergeAsyncApiSpec;
    private Method mergeObjectField;

    @BeforeEach
    void setUp() throws Exception {
        service = new ApiSpecificationExportService("/qip-routes", null, null, null);

        mergeAsyncApiSpec = ApiSpecificationExportService.class
                .getDeclaredMethod("mergeAsyncApiSpec", ObjectNode.class, ObjectNode.class);
        mergeAsyncApiSpec.setAccessible(true);

        mergeObjectField = ApiSpecificationExportService.class
                .getDeclaredMethod("mergeObjectField", ObjectNode.class, ObjectNode.class, String.class);
        mergeObjectField.setAccessible(true);
    }

    @Test
    void mergeObjectFieldAddsNewEntries() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("channels").put("ch1", "v1");

        ObjectNode other = mapper.createObjectNode();
        other.putObject("channels").put("ch2", "v2");

        mergeObjectField.invoke(service, base, other, "channels");

        assertTrue(base.get("channels").has("ch1"));
        assertTrue(base.get("channels").has("ch2"));
    }

    @Test
    void mergeObjectFieldDoesNotOverwriteExisting() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("channels").put("ch1", "original");

        ObjectNode other = mapper.createObjectNode();
        other.putObject("channels").put("ch1", "overwritten");

        mergeObjectField.invoke(service, base, other, "channels");

        assertEquals("original", base.get("channels").get("ch1").asText());
    }

    @Test
    void mergeObjectFieldCreatesFieldIfBaseDoesNotHaveIt() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        ObjectNode other = mapper.createObjectNode();
        other.putObject("servers").put("prod", "host:5672");

        mergeObjectField.invoke(service, base, other, "servers");

        assertTrue(base.has("servers"));
        assertEquals("host:5672", base.get("servers").get("prod").asText());
    }

    @Test
    void mergeObjectFieldSkipsIfOtherDoesNotHaveField() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("channels").put("ch1", "v1");

        ObjectNode other = mapper.createObjectNode();

        mergeObjectField.invoke(service, base, other, "channels");

        assertTrue(base.get("channels").has("ch1"));
    }

    @Test
    void mergeObjectFieldSkipsIfOtherFieldIsNotObject() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("channels").put("ch1", "v1");

        ObjectNode other = mapper.createObjectNode();
        other.put("channels", "not-an-object");

        mergeObjectField.invoke(service, base, other, "channels");

        assertTrue(base.get("channels").has("ch1"));
    }

    @Test
    void mergeObjectFieldSkipsIfBaseFieldIsNotObject() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.put("channels", "not-an-object");

        ObjectNode other = mapper.createObjectNode();
        other.putObject("channels").put("ch2", "v2");

        mergeObjectField.invoke(service, base, other, "channels");

        assertEquals("not-an-object", base.get("channels").asText());
    }

    @Test
    void mergeAsyncApiSpecMergesAllTopLevelFields() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("servers").put("s1", "host1");
        base.putObject("channels").put("ch1", "addr1");

        ObjectNode other = mapper.createObjectNode();
        other.putObject("servers").put("s2", "host2");
        other.putObject("channels").put("ch2", "addr2");
        other.putObject("operations").put("op1", "data");

        mergeAsyncApiSpec.invoke(service, base, other);

        assertTrue(base.get("servers").has("s1"));
        assertTrue(base.get("servers").has("s2"));
        assertTrue(base.get("channels").has("ch1"));
        assertTrue(base.get("channels").has("ch2"));
        assertTrue(base.has("operations"));
        assertTrue(base.get("operations").has("op1"));
    }

    @Test
    void mergeAsyncApiSpecMergesComponentsSubFields() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        ObjectNode baseComponents = base.putObject("components");
        baseComponents.putObject("schemas").put("A", "schemaA");

        ObjectNode other = mapper.createObjectNode();
        ObjectNode otherComponents = other.putObject("components");
        otherComponents.putObject("schemas").put("B", "schemaB");
        otherComponents.putObject("messages").put("M1", "msg1");

        mergeAsyncApiSpec.invoke(service, base, other);

        assertTrue(base.at("/components/schemas").has("A"));
        assertTrue(base.at("/components/schemas").has("B"));
        assertTrue(base.at("/components/messages").has("M1"));
    }

    @Test
    void mergeAsyncApiSpecSetsComponentsIfBaseLacksThem() throws Exception {
        ObjectNode base = mapper.createObjectNode();

        ObjectNode other = mapper.createObjectNode();
        other.putObject("components").putObject("schemas").put("X", "val");

        mergeAsyncApiSpec.invoke(service, base, other);

        assertTrue(base.has("components"));
        assertTrue(base.at("/components/schemas").has("X"));
    }

    @Test
    void mergeAsyncApiSpecSkipsIfOtherHasNoComponents() throws Exception {
        ObjectNode base = mapper.createObjectNode();
        base.putObject("components").putObject("schemas").put("A", "val");

        ObjectNode other = mapper.createObjectNode();

        mergeAsyncApiSpec.invoke(service, base, other);

        assertTrue(base.at("/components/schemas").has("A"));
    }

    @Test
    void guessFormatDetectsJsonAndYaml() throws Exception {
        Method guessFormat = ApiSpecificationExportService.class
                .getDeclaredMethod("guessFormat", String.class);
        guessFormat.setAccessible(true);

        assertEquals(
                org.qubership.integration.platform.runtime.catalog.model.apispec.ApiSpecificationFormat.JSON,
                guessFormat.invoke(service, "{\"asyncapi\": \"3.0.0\"}")
        );
        assertEquals(
                org.qubership.integration.platform.runtime.catalog.model.apispec.ApiSpecificationFormat.YAML,
                guessFormat.invoke(service, "asyncapi: 3.0.0")
        );
    }
}
