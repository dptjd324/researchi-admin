package com.researchi.admin.export.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.export.service.ExportService;
import com.researchi.admin.job.service.JobService;
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
    private ExportService exportService;
    @Mock
    private JobService jobService;

    @InjectMocks
    private ExportController exportController;

    @Test
    void exportXlsxReturnsStreamingAttachmentResponse() throws Exception {
        when(exportService.describeXlsx(9L))
                .thenReturn(new ExportFileDescriptor("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        doAnswer(invocation -> {
            invocation.getArgument(4, java.io.OutputStream.class).write(new byte[]{1, 2, 3});
            return null;
        }).when(exportService).streamXlsx(eq(9L), eq("job-9.xlsx"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());

        ResponseEntity<StreamingResponseBody> response = exportController.exportXlsx(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("job-9.xlsx");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);
        assertThat(outputStream.toByteArray()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        verify(exportService).streamXlsx(eq(9L), eq("job-9.xlsx"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());
    }

    @Test
    void exportTxtReturnsStreamingAttachmentResponse() throws Exception {
        when(exportService.describeTxt(9L))
                .thenReturn(new ExportFileDescriptor("job-9.txt", "text/plain; charset=UTF-8"));
        doAnswer(invocation -> {
            invocation.getArgument(4, java.io.OutputStream.class).write("header\tvalue".getBytes());
            return null;
        }).when(exportService).streamTxt(eq(9L), eq("job-9.txt"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());

        ResponseEntity<StreamingResponseBody> response = exportController.exportTxt(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("job-9.txt");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);
        assertThat(outputStream.toString()).isEqualTo("header\tvalue");
        verify(exportService).streamTxt(eq(9L), eq("job-9.txt"), any(AdminPrincipal.class), any(HttpServletRequest.class), any());
    }
}
