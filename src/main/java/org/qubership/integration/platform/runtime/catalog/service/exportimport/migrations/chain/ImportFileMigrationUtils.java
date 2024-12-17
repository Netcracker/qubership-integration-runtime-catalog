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

package org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.ImportFileMigration;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.migrations.system.ServiceImportFileMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImportFileMigrationUtils {

    private static List<Integer> actualChainFileMigrationVersions;
    private static List<Integer> actualServiceFileMigrationVersions;

    @Autowired
    public ImportFileMigrationUtils(
            List<ChainImportFileMigration> chainImportFileMigrations,
            List<ServiceImportFileMigration> serviceImportFileMigrations
    ) {
        actualChainFileMigrationVersions = calculateActualFileMigrationVersions(chainImportFileMigrations);
        actualServiceFileMigrationVersions = calculateActualFileMigrationVersions(serviceImportFileMigrations);
    }

    public static List<Integer> getActualChainFileMigrationVersions() {
        return actualChainFileMigrationVersions;
    }

    public static List<Integer> getActualServiceFileMigrationVersions() {
        return actualServiceFileMigrationVersions;
    }

    public static void renameProperty(ObjectNode properties, String propertyNameFrom, String propertyNameTo) {
        JsonNode property = properties.get(propertyNameFrom);
        if (property != null) {
            properties.set(propertyNameTo, property);
            properties.remove(propertyNameFrom);
        }
    }

    private List<Integer> calculateActualFileMigrationVersions(List<? extends ImportFileMigration> importFileMigrations) {
        return importFileMigrations.stream()
                .map(ImportFileMigration::getVersion)
                .toList();
    }
}
