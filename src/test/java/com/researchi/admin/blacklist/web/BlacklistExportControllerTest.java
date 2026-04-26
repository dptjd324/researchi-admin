package com.researchi.admin.blacklist.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.service.BlacklistExportService;
import com.researchi.admin.export.domain.ExportPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistExportControllerTest {

    @Mock
    private BlacklistExportService blacklistExportService;

    @InjectMocks
    private BlacklistExportController blacklistExportController;

    @Test
    void exportXlsxReturnsAttachmentResponse() {
        when(blacklistExportService.exportXlsx(eq("kim"), eq("Y"), eq("PERMANENT_BLOCK"), any(AdminPrincipal.class), any(HttpServletRequest.class)))
                .thenReturn(new ExportPayload("blacklist.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3}, 3));

        ResponseEntity<byte[]> response = blacklistExportController.exportXlsx(
                "kim",
                "Y",
                "PERMANENT_BLOCK",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("blacklist.xlsx");
        assertThat(response.getBody()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        verify(blacklistExportService).exportXlsx(eq("kim"), eq("Y"), eq("PERMANENT_BLOCK"), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }

    @Test
    void exportTxtReturnsAttachmentResponse() {
        when(blacklistExportService.exportTxt(eq("kim"), eq(""), eq(""), any(AdminPrincipal.class), any(HttpServletRequest.class)))
                .thenReturn(new ExportPayload("blacklist.txt", "text/plain; charset=UTF-8", "id\tname".getBytes(StandardCharsets.UTF_8), 1));

        ResponseEntity<byte[]> response = blacklistExportController.exportTxt(
                "kim",
                "",
                "",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("blacklist.txt");
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("id\tname");
        verify(blacklistExportService).exportTxt(eq("kim"), eq(""), eq(""), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }
}
