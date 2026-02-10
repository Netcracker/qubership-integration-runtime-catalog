package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildService;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceOptionsProvider;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceService;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.DeployMode;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceDeployRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1/cr")
@Tag(
        name = "custom-resource-controller",
        description = "Custom Resource Build and Deploy Controller"
)
public class CustomResourceController {
    private final CustomResourceBuildService customResourceBuildService;
    private final CustomResourceService customResourceService;
    private final CustomResourceOptionsProvider customResourceOptionsProvider;

    @Autowired
    public CustomResourceController(
            CustomResourceBuildService customResourceBuildService,
            CustomResourceService customResourceService,
            CustomResourceOptionsProvider customResourceOptionsProvider
    ) {
        this.customResourceBuildService = customResourceBuildService;
        this.customResourceService = customResourceService;
        this.customResourceOptionsProvider = customResourceOptionsProvider;
    }

    @PostMapping(produces = MediaType.APPLICATION_YAML_VALUE)
    @Operation(description = "Build K8s resources for specified chains")
    public String buildResource(@RequestBody ResourceBuildRequest request) {
        log.debug("Request to build a CR for chains: {}", request.getChainIds());
        return customResourceBuildService.buildResources(request);
    }

    @PostMapping("/deploy")
    @Operation(description = "Deploy as Camel-K Integration resource")
    public ResponseEntity<Void> deployResource(@Valid @RequestBody ResourceDeployRequest request) {
        log.debug("Request to deploy a Camel-K custom resource with name {} for chains {} using {} mode.",
                request.getName(), request.getChainIds(), request.getMode());

        ResourceBuildRequest buildRequest = ResourceBuildRequest.builder()
                .options(customResourceOptionsProvider.getOptions(request))
                .chainIds(request.getChainIds())
                .build();
        String resourceText = customResourceBuildService.buildResources(
                buildRequest,
                DeployMode.APPEND.equals(request.getMode()));
        customResourceService.deploy(resourceText);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{name}")
    @Operation(description = "Delete Camel-K Integration resource")
    public ResponseEntity<Void> deleteResource(@PathVariable String name) {
        log.debug("Request to delete a Camel-K custom resource with name {}", name);
        customResourceService.delete(name);
        return ResponseEntity.ok().build();
    }
}
