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

package org.qubership.integration.platform.runtime.catalog.model.mapper.mapping.definition.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.qubership.integration.platform.runtime.catalog.model.mapper.metadata.Metadata;

public class GeneratedValue extends ValueSupplierBase {
    @JsonProperty(value = "generator", required = true)
    private final ValueGenerator generator;

    @JsonCreator
    public GeneratedValue(
            @JsonProperty(value = "generator", required = true) ValueGenerator generator,
            @JsonProperty("metadata") Metadata metadata
    ) {
        super(SupplierKind.GENERATED, metadata);
        this.generator = generator;
    }

    public ValueGenerator getGenerator() {
        return generator;
    }
}
