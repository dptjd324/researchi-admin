package com.researchi.admin.export.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportAnswerSource;
import com.researchi.admin.export.domain.ExportApplicationSource;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportLogMapper;
import com.researchi.admin.export.mapper.AdminExportQueryMapper;
import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.form.domain.FormFieldOption;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.publicform.config.PublicFormProperties;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private JobService jobService;
    @Mock
    private FormFieldService formFieldService;
    @Mock
    private AdminExportQueryMapper adminExportQueryMapper;
    @Mock
    private AdminExportLogMapper adminExportLogMapper;
    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private AdminActionLogService adminActionLogService;

    private ExportService exportService;

    @Test
    void exportTxtBuildsOrderedColumnsWithDecryptedValuesAndLogsHistory() {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(2L, "brands", "Preferred Brands", "CHECKBOX", 2, false, null, null, List.of(), true),
                new FormFieldDetail(1L, "availability", "Availability", "SELECT", 1, true, null, null, List.of(new FormFieldOption("weekday", "Weekday")), true)
        ));
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByDocumentSrl(9L)).thenReturn(List.of(
                answer(101L, 1L, "weekday", null),
                answer(101L, 2L, "BrandA, BrandB", "[\"BrandA\",\"BrandB\"]")
        ));

        ExportPayload payload = exportService.exportTxt(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(payload.fileName()).startsWith("job-9-applications-").endsWith(".txt");
        assertThat(payload.exportedCount()).isEqualTo(1);
        String content = new String(payload.content(), StandardCharsets.UTF_8);
        assertThat(content).contains("\uC21C\uBC88\t\uC131\uBA85");
        assertThat(content).contains("Availability");
        assertThat(content).contains("Preferred Brands");
        assertThat(content).contains("Hong");
        assertThat(content).contains("01012345678");
        assertThat(content).contains("hong@example.com");
        assertThat(content).contains("\uC811\uC218");
        assertThat(content).contains("\uBC1C\uC1A1 \uB300\uAE30");
        assertThat(content).contains("weekday\tBrandA, BrandB");
        verify(adminExportLogMapper).insert(any());
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_EXPORT"), eq("JOB"), eq("9"), eq("Exported TXT applications (1 rows)"), any());
    }

    @Test
    void exportXlsxWritesDynamicHeadersInFieldOrder() throws Exception {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(30L, "second", "Second Question", "TEXT", 2, false, null, null, List.of(), true),
                new FormFieldDetail(20L, "first", "First Question", "TEXT", 1, false, null, null, List.of(), true)
        ));
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByDocumentSrl(9L)).thenReturn(List.of(
                answer(101L, 20L, "One", null),
                answer(101L, 30L, "Two", null)
        ));

        ExportPayload payload = exportService.exportXlsx(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(payload.content()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("\uC21C\uBC88");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(24).getStringCellValue()).isEqualTo("First Question");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(25).getStringCellValue()).isEqualTo("Second Question");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(24).getStringCellValue()).isEqualTo("One");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(25).getStringCellValue()).isEqualTo("Two");
        }
    }

    @Test
    void exportEscapesSpreadsheetFormulaValues() throws Exception {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(20L, "formula", "Formula Answer", "TEXT", 1, false, null, null, List.of(), true)
        ));
        ExportApplicationSource source = exportApplicationSource();
        source.setApplicantName("=HYPERLINK(\"http://example.test\")");
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(source));
        when(adminExportQueryMapper.findAnswersByDocumentSrl(9L)).thenReturn(List.of(
                answer(101L, 20L, "+SUM(1,1)", null)
        ));

        ExportPayload txtPayload = exportService.exportTxt(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );
        String txtContent = new String(txtPayload.content(), StandardCharsets.UTF_8);
        assertThat(txtContent).contains("'=HYPERLINK");
        assertThat(txtContent).contains("'+SUM(1,1)");

        ExportPayload xlsxPayload = exportService.exportXlsx(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxPayload.content()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).startsWith("'=HYPERLINK");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(24).getStringCellValue()).isEqualTo("'+SUM(1,1)");
        }
    }

    @Test
    void exportUsesJobSpecificQuestionDefinitions() {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(99L, "screening", "Screening Answer", "TEXT", 1, false, null, null, List.of(), true)
        ));
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByDocumentSrl(9L)).thenReturn(List.of(
                answer(101L, 99L, "Eligible", null)
        ));

        ExportPayload payload = exportService.exportTxt(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        String content = new String(payload.content(), StandardCharsets.UTF_8);
        assertThat(content).contains("Screening Answer");
        assertThat(content).contains("Eligible");
        verify(formFieldService).getFields(9L);
        verify(adminExportQueryMapper).findAnswersByDocumentSrl(9L);
    }

    @Test
    void exportXlsxWritesHistoryLog() {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of());
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByDocumentSrl(9L)).thenReturn(List.of());

        exportService.exportXlsx(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(adminExportLogMapper, times(1)).insert(any());
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_EXPORT"), eq("JOB"), eq("9"), eq("Exported XLSX applications (1 rows)"), any());
    }

    @Test
    void streamTxtWritesRowsFromPagedQueriesAndLogsHistory() throws Exception {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(20L, "first", "First Question", "TEXT", 1, false, null, null, List.of(), true)
        ));
        when(adminExportQueryMapper.findApplicationsPageByDocumentSrl(9L, 500, 0)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByApplicationIds(List.of(101L))).thenReturn(List.of(answer(101L, 20L, "One", null)));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.streamTxt(
                9L,
                "job-9.txt",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                outputStream
        );

        String content = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(content).contains("\uC21C\uBC88\t\uC131\uBA85");
        assertThat(content).contains("Hong");
        assertThat(content).contains("One");
        verify(adminExportLogMapper).insert(any());
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_EXPORT"), eq("JOB"), eq("9"), eq("Exported TXT applications (1 rows)"), any());
    }

    @Test
    void streamXlsxWritesRowsFromPagedQueries() throws Exception {
        exportService = new ExportService(
                jobService,
                formFieldService,
                adminExportQueryMapper,
                adminExportLogMapper,
                researchApplicationMapper,
                researchMasterService,
                protectionService(),
                adminActionLogService
        );
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getFields(9L)).thenReturn(List.of(
                new FormFieldDetail(20L, "first", "First Question", "TEXT", 1, false, null, null, List.of(), true)
        ));
        when(adminExportQueryMapper.findApplicationsPageByDocumentSrl(9L, 500, 0)).thenReturn(List.of(exportApplicationSource()));
        when(adminExportQueryMapper.findAnswersByApplicationIds(List.of(101L))).thenReturn(List.of(answer(101L, 20L, "One", null)));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.streamXlsx(
                9L,
                "job-9.xlsx",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                outputStream
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(24).getStringCellValue()).isEqualTo("First Question");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("Hong");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(24).getStringCellValue()).isEqualTo("One");
        }
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_EXPORT"), eq("JOB"), eq("9"), eq("Exported XLSX applications (1 rows)"), any());
    }

    private ExportApplicationSource exportApplicationSource() {
        ExportApplicationSource source = new ExportApplicationSource();
        source.setId(101L);
        source.setDocumentSrl(9L);
        source.setApplicantName("Hong");
        source.setGenderCode("F");
        source.setBirthDate(LocalDate.of(1995, 5, 2));
        source.setAgeText("30");
        source.setJobText("Research Participant");
        source.setOrganizationText("Researchi");
        PublicFormProtectionService protectionService = protectionService();
        source.setMobilePhoneEnc(protectionService.encrypt("01012345678"));
        source.setTelPhoneEnc(protectionService.encrypt("0212345678"));
        source.setRegionText("Seoul");
        source.setAddressEnc(protectionService.encrypt("123 Teheran-ro"));
        source.setExtraComment("Available weekdays");
        source.setPriorResearchText("Focus groups");
        source.setEmailAddressEnc(protectionService.encrypt("hong@example.com"));
        source.setNotifyEmailYn("Y");
        source.setNotifySmsYn("N");
        source.setNotifyKeywordYn("Y");
        source.setApplicationStatus("RECEIVED");
        source.setIsNewApplicant("Y");
        source.setIsBlacklisted("N");
        source.setBlackModeApplied(null);
        source.setProvideYn("Y");
        source.setDeliveryStatus("PENDING");
        source.setAppliedAt(LocalDateTime.of(2026, 4, 16, 9, 30));
        return source;
    }

    private ExportAnswerSource answer(Long applicationId, Long fieldId, String answerText, String answerJson) {
        ExportAnswerSource source = new ExportAnswerSource();
        source.setApplicationId(applicationId);
        source.setFieldId(fieldId);
        source.setAnswerText(answerText);
        source.setAnswerJson(answerJson);
        return source;
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Survey Job");
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }

    private PublicFormProtectionService protectionService() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        return new PublicFormProtectionService(properties);
    }
}
