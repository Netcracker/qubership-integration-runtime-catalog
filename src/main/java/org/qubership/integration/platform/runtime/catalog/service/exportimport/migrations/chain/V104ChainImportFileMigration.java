package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static org.qubership.integration.platform.runtime.catalog.service.exportimport.ExportImportConstants.*;

@Slf4j
@Component
public class V104ChainImportFileMigration implements ChainImportFileMigration {
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Override
    public int getVersion() {
        return 104;
    }

    @Override
    public ObjectNode makeMigration(ObjectNode fileNode) throws JsonProcessingException {
        log.debug("Applying chain migration: {}", getVersion());
        processElementsAction(fileNode.get(ELEMENTS));

        return fileNode;
    }

    private void processElementsAction(JsonNode elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }

        for (JsonNode element : elements) {
            processElementsAction(element.get(CHILDREN));
            JsonNode elemType = element.get(ELEMENT_TYPE);
            ObjectNode properties = (ObjectNode) element.get(PROPERTIES);

            if (elemType == null || !"http-trigger".equals(elemType.asText())) {
                continue;
            }

            if (properties == null || !properties.has("accessControlType") || !"ABAC".equals(properties.get("accessControlType").asText())) {
                continue;
            }

            ObjectNode abacParameters = properties.has("abacParameters") && properties.get("abacParameters").isObject()
                    ? (ObjectNode) properties.get("abacParameters")
                    : yamlMapper.createObjectNode();

            if (!abacParameters.has("resourceType")) {
                abacParameters.put("resourceType", "CHAIN");
            }
            if (!abacParameters.has("operation")) {
                abacParameters.put("operation", "ALL");
            }
            if (!abacParameters.has("resourceDataType")) {
                abacParameters.put("resourceDataType", "String");
            }

            if (properties.has("abacResource")) {
                String abacResource = properties.get("abacResource").asText();

                abacParameters.put("resourceString", abacResource);
                properties.remove("abacResource");
            }

            properties.set("abacParameters", abacParameters);
        }
    }
}
