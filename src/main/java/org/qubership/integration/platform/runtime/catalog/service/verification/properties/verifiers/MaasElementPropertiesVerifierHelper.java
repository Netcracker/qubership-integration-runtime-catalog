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

package org.qubership.integration.platform.runtime.catalog.service.verification.properties.verifiers;

import org.qubership.integration.platform.runtime.catalog.model.constant.ConnectionSourceType;
import org.qubership.integration.platform.runtime.catalog.service.verification.properties.VerificationError;
import org.qubership.integration.platform.catalog.model.system.EnvironmentSourceType;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;

public class MaasElementPropertiesVerifierHelper {
    public static Collection<VerificationError> verifyMaasProperties(
            String sourceType,
            String maasClassifier
    ) {
        if ((ConnectionSourceType.MAAS.toString().equals(sourceType)
                || EnvironmentSourceType.MAAS_BY_CLASSIFIER.toString().equals(sourceType))
                && StringUtils.isEmpty(maasClassifier)) {
            return Collections.singletonList(
                    new VerificationError("MaaS classifier name is empty"));
        } else {
            return Collections.emptyList();
        }
    }
}