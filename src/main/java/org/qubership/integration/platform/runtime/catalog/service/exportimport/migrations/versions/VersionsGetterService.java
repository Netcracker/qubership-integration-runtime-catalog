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

package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.versions;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VersionsGetterService {
    private final List<VersionsGetterStrategy> strategies;

    @Autowired
    public VersionsGetterService(List<VersionsGetterStrategy> strategies) {
        this.strategies = strategies;
    }

    public Collection<Integer> getVersions(JsonNode document) throws Exception {
        return strategies.stream()
                .map(strategy -> {
                    log.trace("Applying file migrations getter strategy: {}", strategy.getClass().getName());
                    Optional<List<Integer>> result = strategy.getVersions(document);
                    log.trace("Result: {}", result);
                    return result;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new Exception("Failed to get a migration data"));
    }
}
