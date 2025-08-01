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

package org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneralImportInstructionsConfig {

    @Valid
    @Builder.Default
    private ChainImportInstructionsConfig chains = new ChainImportInstructionsConfig();
    @Valid
    @Builder.Default
    private ImportInstructionsConfig services = new ImportInstructionsConfig();
    @Valid
    @Builder.Default
    @JsonIgnoreProperties(value = "ignore")
    private ImportInstructionsConfig specificationGroups = new ImportInstructionsConfig();
    @Valid
    @Builder.Default
    @JsonIgnoreProperties(value = "ignore")
    private ImportInstructionsConfig specifications = new ImportInstructionsConfig();
    @Valid
    @Builder.Default
    private ImportInstructionsConfig commonVariables = new ImportInstructionsConfig();
    @JsonIgnore
    @Builder.Default
    private Set<String> labels = new HashSet<>();
}
