package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.legacy.research.service.LegacyResearchExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private LegacyResearchExportService legacyResearchExportService;

    @InjectMocks
    private ExportController exportController;

    @Test
    void exportLegacyResearchXlsxReturnsStreamingAttachmentResponse() throws Exception {
        when(legacyResearchExportService.describeXlsx(46408L))
                .thenReturn(new ExportFileDescriptor("research-46408.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        doAnswer(invocation -> {
            invocation.getArgument(4, java.io.OutputStream.class).write(new byte[]{1, 2, 3});
            return null;
        }).when(legacyResearchExportService).streamXlsx(eq(46408L), eq("research-46408.xlsx"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());

        ResponseEntity<StreamingResponseBody> response = exportController.exportLegacyResearchXlsx(
                46408L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("research-46408.xlsx");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        verify(legacyResearchExportService).streamXlsx(eq(46408L), eq("research-46408.xlsx"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());
    }
}
