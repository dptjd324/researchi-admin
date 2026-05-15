package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.export.service.ExportService;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.legacy.research.service.LegacyResearchExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@Controller
public class ExportController {

    private final ExportService exportService;
    private final LegacyResearchExportService legacyResearchExportService;
    private final JobService jobService;

    public ExportController(
            ExportService exportService,
            LegacyResearchExportService legacyResearchExportService,
            JobService jobService
    ) {
        this.exportService = exportService;
        this.legacyResearchExportService = legacyResearchExportService;
        this.jobService = jobService;
    }

    @PostMapping("/jobs/{documentSrl}/export/xlsx")
    public ResponseEntity<StreamingResponseBody> exportXlsx(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        jobService.requireApplicationBoard(documentSrl);
        ExportFileDescriptor descriptor = exportService.describeXlsx(documentSrl);
        return toResponse(
                descriptor,
                outputStream -> exportService.streamXlsx(documentSrl, descriptor.fileName(), principal, request, outputStream)
        );
    }

    @PostMapping("/jobs/{documentSrl}/export/txt")
    public ResponseEntity<StreamingResponseBody> exportTxt(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        jobService.requireApplicationBoard(documentSrl);
        ExportFileDescriptor descriptor = exportService.describeTxt(documentSrl);
        return toResponse(
                descriptor,
                outputStream -> exportService.streamTxt(documentSrl, descriptor.fileName(), principal, request, outputStream)
        );
    }

    @PostMapping("/research/{researchNo}/export/xlsx")
    public ResponseEntity<StreamingResponseBody> exportLegacyResearchXlsx(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ExportFileDescriptor descriptor = legacyResearchExportService.describeXlsx(researchNo);
        return toResponse(
                descriptor,
                outputStream -> legacyResearchExportService.streamXlsx(researchNo, descriptor.fileName(), principal, request, outputStream)
        );
    }

    @PostMapping("/research/{researchNo}/export/provide-xlsx")
    public ResponseEntity<StreamingResponseBody> exportLegacyResearchProvideXlsx(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ExportFileDescriptor descriptor = legacyResearchExportService.describeProvideXlsx(researchNo);
        return toResponse(
                descriptor,
                outputStream -> legacyResearchExportService.streamProvideXlsx(researchNo, descriptor.fileName(), principal, request, outputStream)
        );
    }

    @PostMapping("/research/{researchNo}/export/txt")
    public ResponseEntity<StreamingResponseBody> exportLegacyResearchTxt(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ExportFileDescriptor descriptor = legacyResearchExportService.describeTxt(researchNo);
        return toResponse(
                descriptor,
                outputStream -> legacyResearchExportService.streamTxt(researchNo, descriptor.fileName(), principal, request, outputStream)
        );
    }

    @PostMapping("/research/{researchNo}/export/provide-txt")
    public ResponseEntity<StreamingResponseBody> exportLegacyResearchProvideTxt(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ExportFileDescriptor descriptor = legacyResearchExportService.describeProvideTxt(researchNo);
        return toResponse(
                descriptor,
                outputStream -> legacyResearchExportService.streamProvideTxt(researchNo, descriptor.fileName(), principal, request, outputStream)
        );
    }

    private ResponseEntity<StreamingResponseBody> toResponse(ExportFileDescriptor descriptor, StreamingResponseBody body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(descriptor.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(descriptor.contentType()))
                .body(body);
    }
}
