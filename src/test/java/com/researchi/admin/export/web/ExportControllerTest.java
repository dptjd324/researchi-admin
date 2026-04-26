package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    @Test
    void exportXlsxReturnsAttachmentResponse() {
        when(exportService.exportXlsx(eq(9L), any(AdminPrincipal.class), any(HttpServletRequest.class)))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3}, 1));

        ResponseEntity<byte[]> response = exportController.exportXlsx(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("job-9.xlsx");
        assertThat(response.getBody()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        verify(exportService).exportXlsx(eq(9L), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }

    @Test
    void exportTxtReturnsAttachmentResponse() {
        when(exportService.exportTxt(eq(9L), any(AdminPrincipal.class), any(HttpServletRequest.class)))
                .thenReturn(new ExportPayload("job-9.txt", "text/plain; charset=UTF-8", "header\tvalue".getBytes(), 1));

        ResponseEntity<byte[]> response = exportController.exportTxt(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("job-9.txt");
        assertThat(new String(response.getBody())).isEqualTo("header\tvalue");
        verify(exportService).exportTxt(eq(9L), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }
}
