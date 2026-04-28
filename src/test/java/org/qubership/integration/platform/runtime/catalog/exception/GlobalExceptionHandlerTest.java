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

package org.qubership.integration.platform.runtime.catalog.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private static final String EXPECTED_SANITIZED_DATA_INTEGRITY_MESSAGE =
            "Invalid request content. One or more fields violate data constraints.";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationException_returnsSanitizedBadRequest() {
        var ex = new DataIntegrityViolationException(
                "could not execute batch: value too long for varchar(255)",
                new RuntimeException("SQL [update catalog.folders ...]"));

        ResponseEntity<ExceptionDTO> response = handler.handleDataIntegrityViolationException(ex);
        ExceptionDTO body = response.getBody();

        assertNotNull(response);
        assertNotNull(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(EXPECTED_SANITIZED_DATA_INTEGRITY_MESSAGE, body.getErrorMessage());
        assertNotNull(body.getErrorDate());
    }
}
