package org.qubership.integration.platform.runtime.catalog.model.exportimport.template;

import org.qubership.integration.platform.catalog.persistence.configs.entity.template.Template;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TemplateDeserializationResult {
    private Template template;
}
