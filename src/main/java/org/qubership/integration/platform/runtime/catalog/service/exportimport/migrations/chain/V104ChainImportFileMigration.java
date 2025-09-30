package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.qubership.integration.platform.runtime.catalog.service.exportimport.ExportImportConstants.*;

@Component
public class V104ChainImportFileMigration implements ChainImportFileMigration {
    ObjectMapper mapper;

    @Autowired
    public V104ChainImportFileMigration(
            ObjectMapper mapper
    ) {
        this.mapper = mapper;
    }

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
                        ObjectNode abacParameters = (ObjectNode) properties.get("abacParameters");

                        if (abacParameters == null) {
                            abacParameters = mapper.createObjectNode();
                            properties.set("abacParameters", abacParameters);
                        }

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
                    }
                }
            }
        }
    }
}
