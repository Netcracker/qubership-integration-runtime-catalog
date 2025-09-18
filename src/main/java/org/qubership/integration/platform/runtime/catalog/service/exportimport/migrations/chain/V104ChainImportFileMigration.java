package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import static org.qubership.integration.platform.runtime.catalog.service.exportimport.ExportImportConstants.*;

/**
 * Related to DB migration V106_000__abac-properties-in-chains.sql
 */
@Component
public class V104ChainImportFileMigration implements ChainImportFileMigration {
    @Override
    public int getVersion() {
        return 104;
    }

    @Override
    public ObjectNode makeMigration(ObjectNode fileNode) throws JsonProcessingException {
        processElementsAction(fileNode.get(ELEMENTS));

        return fileNode;
    }

    private void processElementsAction(JsonNode elements) {
        if (elements != null && !elements.isEmpty()) {
            for (JsonNode element : elements) {
                processElementsAction(element.get(CHILDREN));
                JsonNode elemType = element.get(ELEMENT_TYPE);
                if (elemType != null && "http-trigger".equals(elemType.asText())) {
                    ObjectNode properties = (ObjectNode) element.get(PROPERTIES);
                    if (properties.has("accessControlType") && "ABAC".equals(properties.get("accessControlType").asText())) {
                        if (!properties.has("abacResourceType")) {
                            properties.put("abacResourceType", "CHAIN");
                        }
                        if (!properties.has("abacOperation")) {
                            properties.put("abacOperation", "ALL");
                        }
                        if (!properties.has("abacResourceDataType")) {
                            properties.put("abacResourceDataType", "String");
                        }
                    }
                }
            }
        }
    }
}
