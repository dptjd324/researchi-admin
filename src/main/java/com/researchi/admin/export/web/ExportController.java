package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.export.service.ExportService;
import com.researchi.admin.job.service.JobService;
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

@Controller
public class ExportController {

    private final ExportService exportService;
    private final JobService jobService;

    public ExportController(ExportService exportService, JobService jobService) {
        this.exportService = exportService;
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

    private ResponseEntity<StreamingResponseBody> toResponse(ExportFileDescriptor descriptor, StreamingResponseBody body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(descriptor.fileName()).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(descriptor.contentType()))
                .body(body);
    }
}
