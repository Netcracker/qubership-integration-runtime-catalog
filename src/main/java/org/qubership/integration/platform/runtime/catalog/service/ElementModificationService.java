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

package org.qubership.integration.platform.runtime.catalog.service;

import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.Operation;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.SpecificationGroup;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.system.SystemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ElementModificationService {
    private final SpecificationGroupService specificationGroupService;
    private final ElementService elementService;

    @Autowired
    public ElementModificationService(SpecificationGroupService specificationGroupService, ElementService elementService) {
        this.specificationGroupService = specificationGroupService;
        this.elementService = elementService;
    }

    public void makeHttpTriggersTypeImplemented(List<String> httpTriggerIds, String specificationGroupId) {
        SpecificationGroup specificationGroup = this.specificationGroupService.getById(specificationGroupId);
        List<ChainElement> chainElements = httpTriggerIds.stream().map(this.elementService::findById).toList();

        if (specificationGroup != null) {
            SystemModel latestSystemModel = specificationGroup.getSystemModels().stream()
                    .max(Comparator.comparing(SystemModel::getCreatedWhen))
                    .orElse(null);

            for (ChainElement element : chainElements) {
                Map<String, Object> elementProperties = element.getProperties();
                Operation operation = null;

                if (latestSystemModel != null) {
                    operation = latestSystemModel.getOperations().stream()
                            .filter(o -> o.getName().contains(element.getId())).findFirst().orElse(null);
                    elementProperties.put("integrationSpecificationId", latestSystemModel.getId());
                }

                if (operation != null) {
                    elementProperties.put("integrationOperationId", operation.getId());
                    elementProperties.put("integrationOperationPath", operation.getPath());
                    elementProperties.put("httpMethodRestrict", operation.getMethod());
                }

                elementProperties.put("systemType", "IMPLEMENTED");
                elementProperties.put("integrationSystemId", specificationGroup.getSystem().getId());
                elementProperties.put("integrationSpecificationGroupId", specificationGroup.getId());
                elementProperties.put("contextPath", null);

                element.setProperties(elementProperties);
                this.elementService.save(element);
            }
        }
    }
}
