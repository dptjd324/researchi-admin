package com.researchi.admin.blacklist.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.service.BlacklistExportService;
import com.researchi.admin.export.domain.ExportPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/blacklist/export")
public class BlacklistExportController {

    private final BlacklistExportService blacklistExportService;

    public BlacklistExportController(BlacklistExportService blacklistExportService) {
        this.blacklistExportService = blacklistExportService;
    }

    @PostMapping("/xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "activeYn", required = false) String activeYn,
            @RequestParam(name = "blackMode", required = false) String blackMode,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return toResponse(blacklistExportService.exportXlsx(keyword, activeYn, blackMode, principal, request));
    }

    @PostMapping("/txt")
    public ResponseEntity<byte[]> exportTxt(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "activeYn", required = false) String activeYn,
            @RequestParam(name = "blackMode", required = false) String blackMode,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return toResponse(blacklistExportService.exportTxt(keyword, activeYn, blackMode, principal, request));
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
