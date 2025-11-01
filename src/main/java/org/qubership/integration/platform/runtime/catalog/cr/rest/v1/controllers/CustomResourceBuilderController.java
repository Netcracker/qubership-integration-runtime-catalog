package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildService;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.CustomResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.repository.chain.ChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1/cr")
@Tag(
        name = "custom-resource-builder-controller",
        description = "Custom Resource Builder Controller"
)
public class CustomResourceBuilderController {
    private final ChainRepository chainRepository;
    private final CustomResourceBuildService customResourceBuildService;

    @Autowired
    public CustomResourceBuilderController(
            ChainRepository chainRepository,
            CustomResourceBuildService customResourceBuildService
    ) {
        this.chainRepository = chainRepository;
        this.customResourceBuildService = customResourceBuildService;
    }

    @PostMapping(produces = MediaType.APPLICATION_YAML_VALUE)
    @Operation(description = "Build CR for specified snapshots")
    public String buildCustomResource(@RequestBody CustomResourceBuildRequest request) {
        log.debug("Request to build a CR for chains: {}", request.getChainIds());
        List<Chain> chains = chainRepository.findAllByIdIn(request.getChainIds());
        return customResourceBuildService.buildCustomResource(
                chains,
                request.getOptions()
        );
    }
}
