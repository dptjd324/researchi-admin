package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/jobs/{documentSrl}/export/xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return toResponse(exportService.exportXlsx(documentSrl, principal, request));
    }

    @GetMapping("/jobs/{documentSrl}/export/txt")
    public ResponseEntity<byte[]> exportTxt(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return toResponse(exportService.exportTxt(documentSrl, principal, request));
    }

    private ResponseEntity<byte[]> toResponse(ExportPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(payload.fileName()).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(payload.content().length)
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }
}
