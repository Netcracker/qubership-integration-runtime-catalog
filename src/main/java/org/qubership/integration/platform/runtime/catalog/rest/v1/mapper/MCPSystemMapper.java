package org.qubership.integration.platform.runtime.catalog.rest.v1.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.mcp.MCPSystem;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.mcp.MCPSystemLabel;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.system.SystemLabelDTO;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.system.mcp.MCPSystemResponseDTO;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.system.mcp.MCPSystemUpdateRequestDTO;
import org.qubership.integration.platform.runtime.catalog.util.MapperUtils;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED,
        uses = {
                MapperUtils.class
        }
)
public interface MCPSystemMapper {
    List<MCPSystemResponseDTO> toResponseDtos(List<MCPSystem> systems);

    MCPSystemResponseDTO toResponseDto(MCPSystem system);

    MCPSystem update(@MappingTarget MCPSystem contextSystem, MCPSystemUpdateRequestDTO request);

    MCPSystemLabel asLabel(SystemLabelDTO labelDTO);

    List<MCPSystemLabel> asLabels(List<SystemLabelDTO> labelDTOs);

    SystemLabelDTO asLabelDTO(MCPSystemLabel label);

    List<SystemLabelDTO> asLabelDTOs(List<MCPSystemLabel> labels);
}
