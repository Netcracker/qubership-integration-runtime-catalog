package org.qubership.integration.platform.runtime.catalog.cr.rest.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceBuildService;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceOptionsProvider;
import org.qubership.integration.platform.runtime.catalog.cr.CustomResourceService;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.DeployMode;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.DeployWithSnapshotCreationRequest;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceBuildRequest;
import org.qubership.integration.platform.runtime.catalog.cr.rest.v1.dto.ResourceDeployRequest;
import org.qubership.integration.platform.runtime.catalog.model.domains.DomainType;
import org.qubership.integration.platform.runtime.catalog.model.domains.EngineDomain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Snapshot;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.repository.chain.ChainRepository;
import org.qubership.integration.platform.runtime.catalog.service.DeploymentService;
import org.qubership.integration.platform.runtime.catalog.service.EngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

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
    private final DeploymentService deploymentService;
    private final ChainRepository chainRepository;
    private final EngineService engineService;

    @Autowired
    public CustomResourceController(
            CustomResourceBuildService customResourceBuildService,
            CustomResourceService customResourceService,
            CustomResourceOptionsProvider customResourceOptionsProvider,
            DeploymentService deploymentService,
            ChainRepository chainRepository,
            EngineService engineService
    ) {
        this.customResourceBuildService = customResourceBuildService;
        this.customResourceService = customResourceService;
        this.customResourceOptionsProvider = customResourceOptionsProvider;
        this.deploymentService = deploymentService;
        this.chainRepository = chainRepository;
        this.engineService = engineService;
    }

    @PostMapping(produces = MediaType.APPLICATION_YAML_VALUE)
    @Operation(description = "Build K8s resources for specified chain snapshots")
    public String buildResource(@RequestBody ResourceBuildRequest request) {
        log.debug("Request to build a CR for snapshots: {}", request.getSnapshotIds());
        return customResourceBuildService.buildResources(request);
    }

    @PostMapping("/deploy-chains")
    @Operation(description = "Deploy with creation of snapshots as Camel-K Integration resource")
    @Transactional
    public ResponseEntity<Void> deployChains(
            @Valid @RequestBody DeployWithSnapshotCreationRequest request
    ) {
        Collection<Chain> chains = chainRepository.findAllById(request.getChainIds()).stream()
                .filter(chain -> isNull(chain.getOverriddenByChainId()))
                .toList();
        Collection<Snapshot> snapshots = deploymentService.provideSnapshots(
                chains.stream().map(Chain::getId).toList(),
                request.getSnapshotAction(),
                (chainId, msg) -> {})
                .values();

        Map<String, DomainType> domainTypeMap = engineService.getDomains().stream()
                .collect(Collectors.toMap(
                        EngineDomain::getName,
                        EngineDomain::getType
                ));
        Map<DomainType, List<String>> domainByType = request.getDomains()
                .stream()
                .collect(Collectors.groupingBy(
                        name -> domainTypeMap.getOrDefault(name, DomainType.MICRO)));

        snapshots.forEach(snapshot -> deploymentService.deploySnapshot(
                snapshot,
                domainByType.getOrDefault(DomainType.NATIVE, Collections.emptyList())));

        domainByType.getOrDefault(DomainType.MICRO, Collections.emptyList()).forEach(name -> {
            doDeployResource(ResourceDeployRequest.builder()
                    .name(name)
                    .mode(request.getMode())
                    .snapshotIds(snapshots.stream().map(Snapshot::getId).toList())
                    .build());
        });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/deploy")
    @Operation(description = "Deploy as Camel-K Integration resource")
    public ResponseEntity<Void> deployResource(@Valid @RequestBody ResourceDeployRequest request) {
        log.debug("Request to deploy a Camel-K custom resource with name {} for chain snapshots {} using {} mode.",
                request.getName(), request.getSnapshotIds(), request.getMode());
        doDeployResource(request);
        return ResponseEntity.ok().build();
    }

    private void doDeployResource(ResourceDeployRequest request) {
        ResourceBuildRequest buildRequest = ResourceBuildRequest.builder()
                .options(customResourceOptionsProvider.getOptions(request))
                .snapshotIds(request.getSnapshotIds())
                .build();
        String resourceText = customResourceBuildService.buildResources(
                buildRequest,
                DeployMode.APPEND.equals(request.getMode()));
        customResourceService.deploy(resourceText);
    }

    @DeleteMapping("/{name}")
    @Operation(description = "Delete Camel-K Integration resource")
    public ResponseEntity<Void> deleteResource(@PathVariable String name) {
        log.debug("Request to delete a Camel-K custom resource with name {}", name);
        customResourceService.delete(name);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{name}/{snapshotId}")
    @Operation(description = "Delete integration chain snapshot from Camel-K resource")
    public ResponseEntity<Void> deleteSnapshotFromResource(@PathVariable String name, @PathVariable String snapshotId) {
        log.debug("Request to delete chain snapshot {} from a Camel-K custom resource {}", snapshotId, name);
        customResourceService.deleteChainSnapshot(name, snapshotId);
        return ResponseEntity.ok().build();
    }
}
