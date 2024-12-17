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

package org.qubership.integration.platform.runtime.catalog.builder.templates.helpers;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.qubership.integration.platform.runtime.catalog.builder.templates.TemplatesHelper;

import java.io.IOException;

/**
 * Helper which receives property with provided name and put it
 * as handlebars context.
 */
@TemplatesHelper("with-property")
public class WithPropertyHelper extends BaseHelper implements Helper<String> {

    @Override
    public Object apply(String propertyName, Options options) throws IOException {
        var model = options.context.model();
        if (model != null) {
            var result = getPropertyValue(propertyName, options);
            if (result instanceof Iterable) {
                return putCollectionAsContext((Iterable) result, options);
            }
            return putObjectAsChildContext(result, options);
        } else {
            return options.inverse();
        }
    }

}