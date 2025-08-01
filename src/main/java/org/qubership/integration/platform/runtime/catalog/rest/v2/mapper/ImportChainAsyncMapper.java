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

package org.qubership.integration.platform.runtime.catalog.rest.v2.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.qubership.integration.platform.runtime.catalog.model.mapper.mapping.UserMapper;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.ImportSession;
import org.qubership.integration.platform.runtime.catalog.rest.v2.dto.exportimport.chain.ImportAsyncStatus;
import org.qubership.integration.platform.runtime.catalog.util.MapperUtils;

@Mapper(componentModel = "spring",
        uses = {
            MapperUtils.class,
            UserMapper.class
        }
)
public interface ImportChainAsyncMapper {

    @Mapping(target = "done", expression = "java(isDone(importSession))")
    ImportAsyncStatus asImportStatus(ImportSession importSession);

    default boolean isDone(ImportSession importSession) {
        return importSession.isDone();
    }
}
