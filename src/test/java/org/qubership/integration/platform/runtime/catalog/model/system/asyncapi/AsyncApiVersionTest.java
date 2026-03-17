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

package org.qubership.integration.platform.runtime.catalog.model.system.asyncapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncApiVersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"2.0.0", "2.6.0", "2.9.9", "1.0.0"})
    void detectReturnsV2ForNonV3Versions(String version) {
        assertEquals(AsyncApiVersion.V2, AsyncApiVersion.detect(version));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void detectReturnsV2ForNullAndEmpty(String version) {
        assertEquals(AsyncApiVersion.V2, AsyncApiVersion.detect(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.0", "3.1.0", "3.99.0"})
    void detectReturnsV3ForV3Versions(String version) {
        assertEquals(AsyncApiVersion.V3, AsyncApiVersion.detect(version));
    }

    @Test
    void detectReturnsV2ForArbitraryString() {
        assertEquals(AsyncApiVersion.V2, AsyncApiVersion.detect("not-a-version"));
    }
}
