package com.researchi.admin.export.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.AdminExportLog;
import com.researchi.admin.export.domain.ExportAnswerSource;
import com.researchi.admin.export.domain.ExportApplicationSource;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportLogMapper;
import com.researchi.admin.export.mapper.AdminExportQueryMapper;
import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class ExportService {

    private static final int EXPORT_BATCH_SIZE = 500;
    private static final int XLSX_STREAM_WINDOW_SIZE = 100;
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TXT_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter EXPORTED_DT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    private static final DateTimeFormatter EXPORTED_DATE = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final List<ColumnDefinition> COMMON_COLUMNS = List.of(
            new ColumnDefinition("\uC21C\uBC88", row -> stringValue(row.sequence())),
            new ColumnDefinition("\uC131\uBA85", ExportRow::applicantName),
            new ColumnDefinition("\uC131\uBCC4", ExportRow::genderCode),
            new ColumnDefinition("\uC0DD\uB144\uC6D4\uC77C", row -> stringValue(row.birthDate())),
            new ColumnDefinition("\uB9CC\uB098\uC774", ExportRow::ageText),
            new ColumnDefinition("\uC9C1\uC5C5", ExportRow::jobText),
            new ColumnDefinition("\uD68C\uC0AC/\uD559\uAD50\uBA85", ExportRow::organizationText),
            new ColumnDefinition("\uD734\uB300\uC804\uD654", ExportRow::mobilePhone),
            new ColumnDefinition("\uC804\uD654\uBC88\uD638", ExportRow::telPhone),
            new ColumnDefinition("\uC9C0\uC5ED", ExportRow::regionText),
            new ColumnDefinition("\uC8FC\uC18C", ExportRow::address),
            new ColumnDefinition("\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D", ExportRow::extraComment),
            new ColumnDefinition("\uAE30\uC874 \uC870\uC0AC \uACBD\uD5D8", ExportRow::priorResearchText),
            new ColumnDefinition("\uC774\uBA54\uC77C", ExportRow::emailAddress),
            new ColumnDefinition("\uC774\uBA54\uC77C \uC54C\uB9BC \uB3D9\uC758", row -> yesNoLabel(row.notifyEmailYn())),
            new ColumnDefinition("SMS \uC54C\uB9BC \uB3D9\uC758", row -> yesNoLabel(row.notifySmsYn())),
            new ColumnDefinition("\uD0A4\uC6CC\uB4DC \uC54C\uB9BC \uB3D9\uC758", row -> yesNoLabel(row.notifyKeywordYn())),
            new ColumnDefinition("\uC9C0\uC6D0\uC11C \uC0C1\uD0DC", row -> applicationStatusLabel(row.applicationStatus())),
            new ColumnDefinition("\uC2E0\uADDC \uC9C0\uC6D0\uC790", row -> yesNoLabel(row.isNewApplicant())),
            new ColumnDefinition("\uBE14\uB799\uB9AC\uC2A4\uD2B8 \uC5EC\uBD80", row -> yesNoLabel(row.isBlacklisted())),
            new ColumnDefinition("\uBE14\uB799\uB9AC\uC2A4\uD2B8 \uBAA8\uB4DC", row -> blacklistModeLabel(row.blackModeApplied())),
            new ColumnDefinition("\uAC1C\uC778\uC815\uBCF4 \uB3D9\uC758", row -> yesNoLabel(row.provideYn())),
            new ColumnDefinition("\uBC1C\uC1A1 \uC0C1\uD0DC", row -> deliveryStatusLabel(row.deliveryStatus())),
            new ColumnDefinition("\uC9C0\uC6D0 \uC2DC\uAC01", row -> stringValue(row.appliedAt()))
    );

    private final JobService jobService;
    private final FormFieldService formFieldService;
    private final AdminExportQueryMapper adminExportQueryMapper;
    private final AdminExportLogMapper adminExportLogMapper;
    private final PublicFormProtectionService protectionService;
    private final AdminActionLogService adminActionLogService;

    public ExportService(
            JobService jobService,
            FormFieldService formFieldService,
            AdminExportQueryMapper adminExportQueryMapper,
            AdminExportLogMapper adminExportLogMapper,
            PublicFormProtectionService protectionService,
            AdminActionLogService adminActionLogService
    ) {
        this.jobService = jobService;
        this.formFieldService = formFieldService;
        this.adminExportQueryMapper = adminExportQueryMapper;
        this.adminExportLogMapper = adminExportLogMapper;
        this.protectionService = protectionService;
        this.adminActionLogService = adminActionLogService;
    }

    @Transactional("adminTransactionManager")
    public ExportPayload exportXlsx(Long documentSrl, AdminPrincipal principal, HttpServletRequest request) {
        ExportPayload payload = prepareXlsx(documentSrl, null);
        writeLogs(documentSrl, "XLSX", payload.fileName(), payload.exportedCount(), principal, request);
        return payload;
    }

    @Transactional("adminTransactionManager")
    public ExportPayload exportTxt(Long documentSrl, AdminPrincipal principal, HttpServletRequest request) {
        ExportPayload payload = prepareTxt(documentSrl, null);
        writeLogs(documentSrl, "TXT", payload.fileName(), payload.exportedCount(), principal, request);
        return payload;
    }

    public ExportPayload prepareXlsx(Long documentSrl, List<Long> applicationIds) {
        ExportContext context = buildContext(documentSrl, applicationIds);
        byte[] content = buildXlsx(context);
        String fileName = buildFileName(documentSrl, "xlsx");
        return new ExportPayload(
                fileName,
                XLSX_CONTENT_TYPE,
                content,
                context.rows().size()
        );
    }

    public ExportPayload prepareTxt(Long documentSrl, List<Long> applicationIds) {
        ExportContext context = buildContext(documentSrl, applicationIds);
        byte[] content = buildTxt(context);
        String fileName = buildFileName(documentSrl, "txt");
        return new ExportPayload(fileName, TXT_CONTENT_TYPE, content, context.rows().size());
    }

    public ExportFileDescriptor describeXlsx(Long documentSrl) {
        return new ExportFileDescriptor(buildFileName(documentSrl, "xlsx"), XLSX_CONTENT_TYPE);
    }

    public ExportFileDescriptor describeTxt(Long documentSrl) {
        return new ExportFileDescriptor(buildFileName(documentSrl, "txt"), TXT_CONTENT_TYPE);
    }

    @Transactional("adminTransactionManager")
    public void streamXlsx(
            Long documentSrl,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        int exportedCount = writeXlsxStreaming(documentSrl, outputStream);
        writeLogs(documentSrl, "XLSX", fileName, exportedCount, principal, request);
    }

    @Transactional("adminTransactionManager")
    public void streamTxt(
            Long documentSrl,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        int exportedCount = writeTxtStreaming(documentSrl, outputStream);
        writeLogs(documentSrl, "TXT", fileName, exportedCount, principal, request);
    }

    private ExportContext buildContext(Long documentSrl, List<Long> applicationIds) {
        JobDetail jobDetail = jobService.getJob(documentSrl);
        List<FormFieldDetail> fields = formFieldService.getFields(documentSrl).stream()
                .sorted(Comparator.comparing(FormFieldDetail::fieldOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FormFieldDetail::id))
                .toList();
        Set<Long> includedApplicationIds = applicationIds == null || applicationIds.isEmpty()
                ? null
                : new HashSet<>(applicationIds);

        Map<Long, Map<Long, String>> answersByApplicationId = new LinkedHashMap<>();
        for (ExportAnswerSource answer : adminExportQueryMapper.findAnswersByDocumentSrl(documentSrl)) {
            if (includedApplicationIds != null && !includedApplicationIds.contains(answer.getApplicationId())) {
                continue;
            }
            answersByApplicationId.computeIfAbsent(answer.getApplicationId(), ignored -> new LinkedHashMap<>())
                    .put(answer.getFieldId(), toDisplayAnswer(answer.getAnswerText(), answer.getAnswerJson()));
        }

        List<ExportApplicationSource> applications = adminExportQueryMapper.findApplicationsByDocumentSrl(documentSrl).stream()
                .filter(application -> includedApplicationIds == null || includedApplicationIds.contains(application.getId()))
                .toList();
        List<ExportRow> rows = new ArrayList<>();
        for (int index = 0; index < applications.size(); index++) {
            ExportApplicationSource application = applications.get(index);
            rows.add(toRow(index + 1, application, fields, answersByApplicationId.getOrDefault(application.getId(), Map.of())));
        }

        List<ColumnDefinition> columns = new ArrayList<>(COMMON_COLUMNS);
        for (FormFieldDetail field : fields) {
            columns.add(new ColumnDefinition(field.fieldLabel(), row -> row.dynamicAnswers().getOrDefault(field.id(), "")));
        }
        return new ExportContext(jobDetail.getDocument().getTitle(), columns, rows);
    }

    private ExportLayout buildLayout(Long documentSrl) {
        jobService.getJob(documentSrl);
        List<FormFieldDetail> fields = formFieldService.getFields(documentSrl).stream()
                .sorted(Comparator.comparing(FormFieldDetail::fieldOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FormFieldDetail::id))
                .toList();
        List<ColumnDefinition> columns = new ArrayList<>(COMMON_COLUMNS);
        for (FormFieldDetail field : fields) {
            columns.add(new ColumnDefinition(field.fieldLabel(), row -> row.dynamicAnswers().getOrDefault(field.id(), "")));
        }
        return new ExportLayout(fields, columns);
    }

    private void writeExportRows(Long documentSrl, List<FormFieldDetail> fields, ExportRowConsumer consumer) throws IOException {
        int offset = 0;
        int sequence = 1;
        while (true) {
            List<ExportApplicationSource> applications = adminExportQueryMapper.findApplicationsPageByDocumentSrl(
                    documentSrl,
                    EXPORT_BATCH_SIZE,
                    offset
            );
            if (applications.isEmpty()) {
                return;
            }

            Map<Long, Map<Long, String>> answersByApplicationId = answersByApplicationId(
                    applications.stream().map(ExportApplicationSource::getId).toList()
            );
            for (ExportApplicationSource application : applications) {
                consumer.accept(toRow(
                        sequence++,
                        application,
                        fields,
                        answersByApplicationId.getOrDefault(application.getId(), Map.of())
                ));
            }
            if (applications.size() < EXPORT_BATCH_SIZE) {
                return;
            }
            offset += EXPORT_BATCH_SIZE;
        }
    }

    private Map<Long, Map<Long, String>> answersByApplicationId(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<Long, String>> answersByApplicationId = new LinkedHashMap<>();
        for (ExportAnswerSource answer : adminExportQueryMapper.findAnswersByApplicationIds(applicationIds)) {
            answersByApplicationId.computeIfAbsent(answer.getApplicationId(), ignored -> new LinkedHashMap<>())
                    .put(answer.getFieldId(), toDisplayAnswer(answer.getAnswerText(), answer.getAnswerJson()));
        }
        return answersByApplicationId;
    }

    private ExportRow toRow(
            int sequence,
            ExportApplicationSource application,
            List<FormFieldDetail> fields,
            Map<Long, String> answersByFieldId
    ) {
        Map<Long, String> orderedAnswers = new LinkedHashMap<>();
        for (FormFieldDetail field : fields) {
            orderedAnswers.put(field.id(), answersByFieldId.getOrDefault(field.id(), ""));
        }
        return new ExportRow(
                sequence,
                application.getApplicantName(),
                application.getGenderCode(),
                application.getBirthDate(),
                application.getAgeText(),
                application.getJobText(),
                application.getOrganizationText(),
                protectionService.decrypt(application.getMobilePhoneEnc()),
                protectionService.decrypt(application.getTelPhoneEnc()),
                application.getRegionText(),
                protectionService.decrypt(application.getAddressEnc()),
                application.getExtraComment(),
                application.getPriorResearchText(),
                protectionService.decrypt(application.getEmailAddressEnc()),
                application.getNotifyEmailYn(),
                application.getNotifySmsYn(),
                application.getNotifyKeywordYn(),
                application.getApplicationStatus(),
                application.getIsNewApplicant(),
                application.getIsBlacklisted(),
                application.getBlackModeApplied(),
                application.getProvideYn(),
                application.getDeliveryStatus(),
                application.getAppliedAt(),
                orderedAnswers
        );
    }

    private byte[] buildXlsx(ExportContext context) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Applications");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < context.columns().size(); index++) {
                headerRow.createCell(index).setCellValue(context.columns().get(index).header());
            }
            int rowIndex = 1;
            for (ExportRow exportRow : context.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int columnIndex = 0; columnIndex < context.columns().size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(sanitizeSpreadsheetValue(context.columns().get(columnIndex).value(exportRow)));
                }
            }
            for (int index = 0; index < context.columns().size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("XLSX 파일을 생성하지 못했습니다.", ex);
        }
    }

    private byte[] buildTxt(ExportContext context) {
        StringBuilder builder = new StringBuilder();
        appendTxtLine(builder, context.columns().stream().map(ColumnDefinition::header).toList());
        for (ExportRow row : context.rows()) {
            appendTxtLine(builder, context.columns().stream().map(column -> column.value(row)).toList());
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private int writeXlsxStreaming(Long documentSrl, OutputStream outputStream) {
        ExportLayout layout = buildLayout(documentSrl);
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(XLSX_STREAM_WINDOW_SIZE)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Applications");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < layout.columns().size(); index++) {
                headerRow.createCell(index).setCellValue(layout.columns().get(index).header());
            }

            RowCounter counter = new RowCounter();
            writeExportRows(documentSrl, layout.fields(), exportRow -> {
                Row row = sheet.createRow(counter.value() + 1);
                for (int columnIndex = 0; columnIndex < layout.columns().size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(sanitizeSpreadsheetValue(layout.columns().get(columnIndex).value(exportRow)));
                }
                counter.increment();
            });

            workbook.write(outputStream);
            outputStream.flush();
            workbook.dispose();
            return counter.value();
        } catch (IOException ex) {
            throw new IllegalStateException("XLSX ?뚯씪???앹꽦?섏? 紐삵뻽?듬땲??", ex);
        }
    }

    private int writeTxtStreaming(Long documentSrl, OutputStream outputStream) {
        ExportLayout layout = buildLayout(documentSrl);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            appendTxtLine(writer, layout.columns().stream().map(ColumnDefinition::header).toList());
            RowCounter counter = new RowCounter();
            writeExportRows(documentSrl, layout.fields(), exportRow -> {
                appendTxtLine(writer, layout.columns().stream().map(column -> column.value(exportRow)).toList());
                counter.increment();
            });
            writer.flush();
            return counter.value();
        } catch (IOException ex) {
            throw new IllegalStateException("TXT ?뚯씪???앹꽦?섏? 紐삵뻽?듬땲??", ex);
        }
    }

    private void appendTxtLine(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append('\t');
            }
            builder.append(sanitizeSpreadsheetValue(sanitizeTxt(values.get(index))));
        }
        builder.append(System.lineSeparator());
    }

    private void appendTxtLine(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write('\t');
            }
            writer.write(sanitizeSpreadsheetValue(sanitizeTxt(values.get(index))));
        }
        writer.write(System.lineSeparator());
    }

    private String sanitizeTxt(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private String sanitizeSpreadsheetValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

    private void writeLogs(
            Long documentSrl,
            String exportType,
            String fileName,
            int exportedCount,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        AdminExportLog exportLog = new AdminExportLog();
        exportLog.setDocumentSrl(documentSrl);
        exportLog.setExportType(exportType);
        exportLog.setFileName(fileName);
        exportLog.setExportedCount(exportedCount);
        adminExportLogMapper.insert(exportLog);

        adminActionLogService.log(
                principal.getId(),
                "APPLICATION_EXPORT",
                "JOB",
                String.valueOf(documentSrl),
                "Exported " + exportType + " applications (" + exportedCount + " rows)",
                request
        );
    }

    private String buildFileName(Long documentSrl, String extension) {
        return "job-" + documentSrl + "-applications-" + LocalDateTime.now().format(FILE_TS) + "." + extension;
    }

    private String toDisplayAnswer(String answerText, String answerJson) {
        if (answerJson == null || answerJson.isBlank()) {
            return nullToEmpty(answerText);
        }
        String trimmed = answerJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return nullToEmpty(answerText);
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return nullToEmpty(answerText);
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;
        for (int index = 0; index < body.length(); index++) {
            char currentChar = body.charAt(index);
            if (escaping) {
                current.append(currentChar);
                escaping = false;
                continue;
            }
            if (currentChar == '\\') {
                escaping = true;
                continue;
            }
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                if (!inQuotes) {
                    values.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            if (inQuotes) {
                current.append(currentChar);
            }
        }
        return values.isEmpty() ? nullToEmpty(answerText) : String.join(", ", values);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(EXPORTED_DT);
        }
        if (value instanceof LocalDate localDate) {
            return localDate.format(EXPORTED_DATE);
        }
        return String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String yesNoLabel(String value) {
        if ("Y".equalsIgnoreCase(value)) {
            return "\uC608";
        }
        if ("N".equalsIgnoreCase(value)) {
            return "\uC544\uB2C8\uC624";
        }
        return stringValue(value);
    }

    private static String applicationStatusLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.toUpperCase()) {
            case "RECEIVED" -> "\uC811\uC218";
            case "REVIEWING" -> "\uAC80\uD1A0\uC911";
            case "APPROVED" -> "\uC2B9\uC778";
            case "REJECTED" -> "\uAC70\uC808";
            case "BLOCKED" -> "\uC81C\uD55C";
            default -> value;
        };
    }

    private static String deliveryStatusLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.toUpperCase()) {
            case "PENDING" -> "\uBC1C\uC1A1 \uB300\uAE30";
            case "SENT" -> "\uBC1C\uC1A1 \uC644\uB8CC";
            case "FAILED" -> "\uBC1C\uC1A1 \uC2E4\uD328";
            case "NO_TARGETS" -> "\uBC1C\uC1A1 \uB300\uC0C1 \uC5C6\uC74C";
            default -> value;
        };
    }

    private static String blacklistModeLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.toUpperCase()) {
            case "BLOCK", "PERMANENT_BLOCK" -> "\uCC28\uB2E8";
            case "TEMPORARY_BLOCK" -> "\uC784\uC2DC \uCC28\uB2E8";
            case "MANUAL_REVIEW" -> "\uC218\uB3D9 \uAC80\uD1A0";
            default -> value;
        };
    }

    private record ColumnDefinition(String header, Function<ExportRow, String> extractor) {
        String value(ExportRow row) {
            String value = extractor.apply(row);
            return value == null ? "" : value;
        }
    }

    private record ExportContext(String jobTitle, List<ColumnDefinition> columns, List<ExportRow> rows) {
    }

    private record ExportLayout(List<FormFieldDetail> fields, List<ColumnDefinition> columns) {
    }

    private static class RowCounter {
        private int value;

        int value() {
            return value;
        }

        void increment() {
            value++;
        }
    }

    @FunctionalInterface
    private interface ExportRowConsumer {
        void accept(ExportRow row) throws IOException;
    }

    private record ExportRow(
            int sequence,
            String applicantName,
            String genderCode,
            LocalDate birthDate,
            String ageText,
            String jobText,
            String organizationText,
            String mobilePhone,
            String telPhone,
            String regionText,
            String address,
            String extraComment,
            String priorResearchText,
            String emailAddress,
            String notifyEmailYn,
            String notifySmsYn,
            String notifyKeywordYn,
            String applicationStatus,
            String isNewApplicant,
            String isBlacklisted,
            String blackModeApplied,
            String provideYn,
            String deliveryStatus,
            LocalDateTime appliedAt,
            Map<Long, String> dynamicAnswers
    ) {
    }
}
