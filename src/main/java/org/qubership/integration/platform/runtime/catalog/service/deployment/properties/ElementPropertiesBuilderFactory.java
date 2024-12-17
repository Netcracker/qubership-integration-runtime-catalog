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

package org.qubership.integration.platform.runtime.catalog.service.deployment.properties;

import org.qubership.integration.platform.runtime.catalog.service.deployment.properties.builders.CompoundPropertiesBuilder;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ElementPropertiesBuilderFactory {
    private final Collection<ElementPropertiesBuilder> builders;
    @Autowired
    ElementPropertiesBuilderFactory(Collection<ElementPropertiesBuilder> builders) {
        this.builders = builders;
    }

    public ElementPropertiesBuilder getElementPropertiesBuilder(ChainElement element) {
        return new CompoundPropertiesBuilder(builders.stream().filter(builder -> builder.applicableTo(element)).toList());
    }
}
