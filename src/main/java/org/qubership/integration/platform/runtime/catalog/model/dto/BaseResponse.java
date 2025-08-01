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

package org.qubership.integration.platform.runtime.catalog.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.qubership.integration.platform.runtime.catalog.model.dto.user.UserDTO;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "Basic response object")
public class BaseResponse {
    @Schema(description = "Id of the entity")
    private String id;
    @Schema(description = "Name of the entity")
    private String name;
    @Schema(description = "Entity description")
    private String description;
    @Schema(description = "Timestamp of object creation")
    private Long createdWhen;
    @Schema(description = "User who created that object")
    private UserDTO createdBy;
    @Schema(description = "Timestamp of object last modification")
    private Long modifiedWhen;
    @Schema(description = "User who last modified that object")
    private UserDTO modifiedBy;

}
