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

package org.qubership.integration.platform.runtime.catalog.util;

import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.Dependency;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.qubership.integration.platform.catalog.persistence.configs.entity.chain.element.ContainerChainElement;
import org.qubership.integration.platform.catalog.service.library.LibraryElementsService;
import org.qubership.integration.platform.catalog.util.DistinctByKey;
import org.qubership.integration.platform.runtime.catalog.service.ElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OldContainerUtils {

    private final LibraryElementsService libraryService;

    @Autowired
    public OldContainerUtils(LibraryElementsService libraryService) {
        this.libraryService = libraryService;
    }

    @Nullable
    public ContainerChainElement getOldContainerParent(ContainerChainElement element) {
        if (element == null || !isOldStyleContainer(element.getType())) {
            return element;
        }

        ContainerChainElement parentElement = element.getParent();
        while (parentElement != null && ElementService.CONTAINER_TYPE_NAME.equals(parentElement.getType())) {
            parentElement = parentElement.getParent();
        }
        return parentElement;
    }

    public List<Dependency> extractOldContainerOutputDependencies(ChainElement element) {
        if (isOldStyleContainer(element.getType())) {
            return ((ContainerChainElement) element).getElements().stream()
                    .map(ChainElement::getOutputDependencies)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public boolean isOldStyleContainer(String elementType) {
        return Optional.ofNullable(libraryService.getElementDescriptor(elementType))
                .map(descriptor -> descriptor.isContainer() && descriptor.isOldStyleContainer())
                .orElse(false);
    }

    public List<ChainElement> getAllOldStyleContainerChildren(@NonNull ChainElement element) {
        if (!isOldStyleContainer(element.getType())) {
            return Collections.emptyList();
        }

        return ((ContainerChainElement) element).getElements().stream()
                .map(this::collectAllOldStyleContainerDependentElements)
                .flatMap(List::stream)
                .filter(DistinctByKey.newInstance(ChainElement::getId))
                .toList();
    }

    private List<ChainElement> collectAllOldStyleContainerDependentElements(ChainElement element) {
        List<ChainElement> result = new ArrayList<>();
        List<ChainElement> elementsTo = element.getOutputDependencies()
                .stream()
                .map(Dependency::getElementTo)
                .toList();
        for (ChainElement elementTo : elementsTo) {
            if (isOldStyleContainer(elementTo.getType())) {
                ((ContainerChainElement) elementTo).getElements().stream()
                        .map(this::collectAllOldStyleContainerDependentElements)
                        .flatMap(List::stream)
                        .forEach(result::add);
            }
            result.addAll(collectAllOldStyleContainerDependentElements(elementTo));
            result.add(elementTo);
        }
        return result;
    }
}
