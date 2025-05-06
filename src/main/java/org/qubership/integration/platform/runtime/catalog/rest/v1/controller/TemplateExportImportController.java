package org.qubership.integration.platform.runtime.catalog.rest.v1.controller;

import java.util.List;

import org.qubership.integration.platform.catalog.service.exportimport.ExportImportUtils;
import org.qubership.integration.platform.runtime.catalog.model.exportimport.template.ImportTemplateResult;
import org.qubership.integration.platform.runtime.catalog.rest.v1.dto.system.imports.ImportSystemStatus;
import org.qubership.integration.platform.runtime.catalog.service.exportimport.TemplateExportImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "template-export-import-controller", description = "Template Export Import Controller")
public class TemplateExportImportController {
    private final TemplateExportImportService templateExportImportService;

    @RequestMapping(method = { RequestMethod.GET,
            RequestMethod.POST }, value = "/export/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(description = "Export templates as an archive")
    public ResponseEntity<Object> exportTemplates(
            @RequestParam(required = false) @Parameter(description = "List of template ids, separated by comma") List<String> templateIds) {
        byte[] zip = templateExportImportService.exportTemplates(templateIds);
        if (zip == null) {
            return ResponseEntity.noContent().build();
        }

        return ExportImportUtils.convertFileToResponse(zip, ExportImportUtils.generateArchiveExportName());
    }

    @Operation(extensions = @Extension(properties = {
            @ExtensionProperty(name = "x-api-kind", value = "bwc") }), description = "Import template from a file")
    @PostMapping(value = "/import/template")
    public ResponseEntity<List<ImportTemplateResult>> importTemplates(
            @RequestParam("file") @Parameter(description = "File") MultipartFile file,
            @RequestParam(required = false) @Parameter(description = "List of template ids, separated by comma") List<String> templateIds) {
        List<ImportTemplateResult> result = templateExportImportService.importTemplates(file, templateIds);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            HttpStatus responseCode = result.stream().anyMatch(dto -> dto.getStatus().equals(ImportSystemStatus.ERROR))
                    ? HttpStatus.MULTI_STATUS
                    : HttpStatus.OK;
            return ResponseEntity.status(responseCode).body(result);
        }
    }
}
