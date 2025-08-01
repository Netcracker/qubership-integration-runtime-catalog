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

package org.qubership.integration.platform.runtime.catalog.rest.v1.dto.diagnostic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.diagnostic.ValidationState;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
@Schema(description = "Validation last execution status")
public class ValidationStatusDTO {
    @Schema(description = "Validation last execution state")
    private ValidationState state;
    @Schema(description = "Validation execution start time")
    private Timestamp startedWhen;
    @Schema(description = "Validation last execution additional message (optional)")
    private String message;
}
