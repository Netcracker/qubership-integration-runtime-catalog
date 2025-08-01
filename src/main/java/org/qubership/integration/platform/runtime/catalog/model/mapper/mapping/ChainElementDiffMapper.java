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

package org.qubership.integration.platform.runtime.catalog.model.mapper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.qubership.integration.platform.runtime.catalog.model.dto.chain.ChainElementDifferenceDTO;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.runtime.catalog.service.difference.DifferenceResult;

import java.util.List;

@Mapper(componentModel = "spring", uses = ChainElementMapper.class)
public interface ChainElementDiffMapper {

    @Mapping(target = "leftElement", source = "leftOperand")
    @Mapping(target = "rightElement", source = "rightOperand")
    ChainElementDifferenceDTO asDto(DifferenceResult<ChainElement> diffResult);

    List<ChainElementDifferenceDTO> asDtoList(List<ChainElement> diffResult);
}
