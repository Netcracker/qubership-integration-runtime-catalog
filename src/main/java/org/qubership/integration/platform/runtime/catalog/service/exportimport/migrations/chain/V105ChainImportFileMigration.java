package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import static org.qubership.integration.platform.runtime.catalog.service.exportimport.ExportImportConstants.*;

@Component
public class V105ChainImportFileMigration implements ChainImportFileMigration {
    @Override
    public int getVersion() {
        return 105;
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
                    if (properties.has("accessControlType") && "RBAC".equals(properties.get("accessControlType").asText())) {
                        if (properties.has("roles")) {
                            JsonNode roles = properties.get("roles");
                            if (roles.isEmpty()) {
                                properties.remove("roles");
                                properties.put("accessControlType", "NONE");
                            }
                        } else {
                            properties.put("accessControlType", "NONE");
                        }
                    }
                }
            }
        }
    }
}
