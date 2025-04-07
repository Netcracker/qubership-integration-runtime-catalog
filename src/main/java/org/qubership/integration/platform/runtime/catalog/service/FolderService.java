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

package org.qubership.integration.platform.runtime.catalog.service;

import jakarta.persistence.EntityNotFoundException;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.ActionLog;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.EntityType;
import org.qubership.integration.platform.catalog.persistence.configs.entity.actionlog.LogOperation;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.Chain;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.FoldableEntity;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.Folder;
import org.qubership.integration.platform.catalog.persistence.configs.repository.chain.ChainRepository;
import org.qubership.integration.platform.catalog.persistence.configs.repository.chain.FolderRepository;
import org.qubership.integration.platform.catalog.service.ActionsLogService;
import org.qubership.integration.platform.runtime.catalog.exception.exceptions.FolderMoveException;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.folder.FolderContentFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Transactional
public class FolderService {
    private static final String FOLDER_WITH_ID_NOT_FOUND_MESSAGE = "Can't find folder with id: ";

    private final FolderRepository folderRepository;
    private final ActionsLogService actionLogger;
    private final ChainRepository chainRepository;
    private final DeploymentService deploymentService;

    private final AuditingHandler auditingHandler;

    @Autowired
    public FolderService(FolderRepository folderRepository,
                         ActionsLogService actionLogger,
                         ChainRepository chainRepository,
                         DeploymentService deploymentService,
                         AuditingHandler jpaAuditingHandler) {
        this.folderRepository = folderRepository;
        this.actionLogger = actionLogger;
        this.chainRepository = chainRepository;
        this.deploymentService = deploymentService;
        this.auditingHandler = jpaAuditingHandler;
    }

    public List<Folder> findAllInRoot() {
        return folderRepository.findAllByParentFolderIsNull();
    }

    public Folder findById(String id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(FOLDER_WITH_ID_NOT_FOUND_MESSAGE + id));
    }

    public Folder findFirstByName(String name, Folder parent) {
        return folderRepository.findFirstByNameAndParentFolder(name, parent);
    }

    public Folder findEntityByIdOrNull(String folderId) {
        return folderId != null ? folderRepository.findById(folderId).orElse(null) : null;
    }

    public Map<String, String> provideNavigationPath(String folderId) {
        var folder = findById(folderId);
        return folder.getAncestors();
    }

    public Folder save(Folder folder, String parentFolderId) {
        auditingHandler.markModified(folder);
        return upsertFolder(folder, parentFolderId);
    }

    public Folder save(Folder folder, Folder parentFolder) {
        if (parentFolder == null) {
            return save(folder, (String) null);
        }
        return save(folder, parentFolder.getId());
    }

    public Folder move(String folderId, String targetFolderId) throws FolderMoveException {
        Folder folder = findById(folderId);
        Folder targetFolder = findEntityByIdOrNull(targetFolderId);
        if (targetFolder != null && checkIfMovingToChild(folder, targetFolder)) {
            throw new FolderMoveException(folder.getName(), targetFolder.getName());
        }
        folder.setParentFolder(targetFolder);
        return folder;
    }

    private boolean checkIfMovingToChild(Folder folder, Folder targetFolder) {
        while (targetFolder.getParentFolder() != null) {
            Folder parentFolder = targetFolder.getParentFolder();
            if (parentFolder.getId().equals(folder.getId())) {
                return true;
            }
            targetFolder = parentFolder;
        }
        return false;
    }

    public Folder update(Folder entityFromDto, String folderId, String parentFolderId) {
        Folder folder = findById(folderId);
        folder.merge(entityFromDto);
        return upsertFolder(folder, parentFolderId);
    }

    private Folder upsertFolder(Folder folder, String parentFolderId) {
        Folder newFolder = folderRepository.save(folder);
        if (parentFolderId != null) {
            Folder parentFolder = findEntityByIdOrNull(parentFolderId);
            parentFolder.addChildFolder(newFolder);
            newFolder = folderRepository.save(newFolder);
        }
        return newFolder;
    }

    public List<Folder> getFoldersHierarchically(List<Chain> relatedChains) {
        List<String> foldersIds = relatedChains
                .stream()
                .map(Chain::getParentFolder)
                .filter(Objects::nonNull)
                .map(Folder::getId)
                .collect(Collectors.toList());
        return folderRepository.getFoldersHierarchically(foldersIds);
    }

    public void deleteById(String folderId) {
        Folder folder = findById(folderId);
        deleteRuntimeDeployments(folder);
        List<FoldableEntity> nestedEntities = findAllNestedFoldableEntity(folderId);
        folderRepository.deleteById(folderId);

        for (FoldableEntity entity : nestedEntities) {
            if (!(entity instanceof Folder)) {
                actionLogger.logAction(ActionLog.builder()
                        .entityType(EntityType.CHAIN)
                        .entityId(entity.getId())
                        .entityName(entity.getName())
                        .parentType(entity.getParentFolder() == null ? null : EntityType.FOLDER)
                        .parentId(entity.getParentFolder() == null ? null : entity.getParentFolder().getId())
                        .parentName(entity.getParentFolder() == null ? null : entity.getParentFolder().getName())
                        .operation(LogOperation.DELETE)
                        .build());
            }
        }
    }

    private void deleteRuntimeDeployments(Folder folder) {
        for (Chain chain : folder.getChainList()) {
            deploymentService.deleteAllByChainId(chain.getId());
        }
        for (Folder subfolder : folder.getFolderList()) {
            deleteRuntimeDeployments(subfolder);
        }
    }

    private List<FoldableEntity> findAllNestedFoldableEntity(String folderId) {
        List<FoldableEntity> entities = new ArrayList<>();
        folderRepository.findById(folderId).ifPresent(value -> collectAllNestedFoldableEntityRecursive(entities, value));
        return entities;
    }

    private void collectAllNestedFoldableEntityRecursive(List<FoldableEntity> entities, Folder folder) {
        entities.add(folder);
        entities.addAll(folder.getChainList());
        for (Folder subfolder : folder.getFolderList()) {
            collectAllNestedFoldableEntityRecursive(entities, subfolder);
        }
    }

    public List<Chain> findNestedChains(String folderId, FolderContentFilter filter) {
        List<Folder> nestedFolders = folderRepository.findNestedFolders(folderId);
        Specification<Chain> specification = (root, query, criteriaBuilder) -> criteriaBuilder.or(
                root.get("parentFolder").in(nestedFolders),
                criteriaBuilder.equal(root.get("parentFolder").get("id"), folderId)
        );
        if (nonNull(filter)) {
            specification = specification.and(filter.getSpecification());
        }
        return chainRepository.findAll(specification);
    }

    public List<Folder> findNestedFolders(String folderId) {
        return folderRepository.findNestedFolders(folderId);
    }

    public List<Folder> findAllFoldersToRootParentFolder(String openedFolderId) {
        return folderRepository.findAllFoldersToRootParentFolder(openedFolderId);
    }

    public Folder setActualizedFolderState(Folder folderState) {
        List<Folder> actualizedFolderList = new LinkedList<>(folderState
                .getFolderList()
                .stream()
                .map(folderRepository::persist)
                .toList());

        folderState.setFolderList(actualizedFolderList);

        if (folderState.getParentFolder() != null) {
           Folder actualizedParentFolder = setActualizedFolderState(folderState.getParentFolder());
           folderState.setParentFolder(actualizedParentFolder);
        }

        return folderRepository.persist(folderState);
    }
}
