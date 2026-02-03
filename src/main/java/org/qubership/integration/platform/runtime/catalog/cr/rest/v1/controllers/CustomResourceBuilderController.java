package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildService;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceDeployService;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceOptionsProvider;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildOptions;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceDeployRequest;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.repository.chain.ChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CustomResourceDeployService customResourceDeployService;
    private final CustomResourceOptionsProvider customResourceOptionsProvider;

    @Autowired
    public CustomResourceBuilderController(
            ChainRepository chainRepository,
            CustomResourceBuildService customResourceBuildService,
            CustomResourceDeployService customResourceDeployService,
            CustomResourceOptionsProvider customResourceOptionsProvider
    ) {
        this.chainRepository = chainRepository;
        this.customResourceBuildService = customResourceBuildService;
        this.customResourceDeployService = customResourceDeployService;
        this.customResourceOptionsProvider = customResourceOptionsProvider;
    }

    @PostMapping(produces = MediaType.APPLICATION_YAML_VALUE)
    @Operation(description = "Build CR for specified snapshots")
    public String buildCustomResource(@RequestBody ResourceBuildRequest request) {
        log.debug("Request to build a CR for chains: {}", request.getChainIds());
        List<Chain> chains = chainRepository.findAllByIdIn(request.getChainIds());
        return customResourceBuildService.buildCustomResource(
                chains,
                request.getOptions()
        );
    }

    @PostMapping("/deploy")
    @Operation(description = "Deploy as Camel-K custom resource")
    public ResponseEntity<Void> deployCustomResource(@Valid @RequestBody ResourceDeployRequest request) {
        log.debug("Request to deploy a Camel-K custom resource with name {} for chains: {}",
                request.getName(), request.getChainIds());

        List<Chain> chains = chainRepository.findAllByIdIn(request.getChainIds());
        ResourceBuildOptions options = customResourceOptionsProvider.getOptions(request);
        String resourceText = customResourceBuildService.buildCustomResource(chains, options);
        customResourceDeployService.deploy(resourceText);

        return ResponseEntity.ok().build();
    }
}
