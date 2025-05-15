package org.qubership.integration.platform.runtime.catalog.model.exportimport.template;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.qubership.integration.platform.runtime.catalog.model.exportimport.ExportableObject;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.serializer.ExportableObjectWriterVisitor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportedTemplate implements ExportableObject {
    private String id;
    private ObjectNode objectNode;

    @Override
    public void accept(ExportableObjectWriterVisitor visitor, ZipOutputStream zipOut, String entryPath) throws IOException {
        visitor.visit(this, zipOut, entryPath);
    }
}
