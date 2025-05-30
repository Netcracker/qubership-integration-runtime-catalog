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

package org.qubership.integration.platform.runtime.catalog.model.exportimport.chain;

import org.qubership.integration.platform.runtime.catalog.model.exportimport.instructions.ImportInstructionResult;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.system.ImportSystemResult;

import java.util.ArrayList;
import java.util.List;

public record ImportSystemsAndInstructionsResult(List<ImportSystemResult> importSystemResults, List<ImportInstructionResult> instructionResults) {

    public ImportSystemsAndInstructionsResult {
        if (importSystemResults == null) {
            importSystemResults = new ArrayList<>();
        }
        if (instructionResults == null) {
            instructionResults = new ArrayList<>();
        }
    }

    public ImportSystemsAndInstructionsResult() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}
