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

package org.qubership.integration.platform.runtime.catalog.model.exportimport;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.chain.ImportChainResult;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions.ImportInstructionResult;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.system.ImportSystemResult;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.variable.ImportVariableResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Import preview result")
public class ImportResult {

    @Builder.Default
    @Schema(description = "List of results by each chain")
    private List<ImportChainResult> chains = new ArrayList<>();
    @Builder.Default
    @Schema(description = "List of results by each service")
    private List<ImportSystemResult> systems = new ArrayList<>();
    @Builder.Default
    @Schema(description = "List of results by each variable")
    private List<ImportVariableResult> variables = new ArrayList<>();
    @Builder.Default
    @Schema(description = "List of results by each instruction")
    private List<ImportInstructionResult> instructionsResult = new ArrayList<>();
}
