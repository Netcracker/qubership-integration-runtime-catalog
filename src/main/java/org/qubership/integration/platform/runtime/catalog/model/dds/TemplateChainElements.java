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

package org.qubership.integration.platform.runtime.catalog.model.dds;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateChainElements {
    private List<TemplateChainElement> httpTriggersImplemented;
    private List<TemplateChainElement> httpTriggers;
    private List<TemplateChainElement> httpServiceCalls;
    private List<TemplateChainElement> mappers;
    private List<TemplateChainElement> withAuthorization;
    private List<TemplateChainElement> withErrorHandling;
}
