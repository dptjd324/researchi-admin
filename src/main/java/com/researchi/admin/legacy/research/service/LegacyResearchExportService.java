package com.researchi.admin.legacy.research.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.AdminExportLog;
import com.researchi.admin.export.domain.ExportFileDescriptor;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportLogMapper;
import com.researchi.admin.legacy.application.support.ApplicationFormNoticeItem;
import com.researchi.admin.legacy.application.support.ApplicationFormNoticeParser;
import com.researchi.admin.legacy.application.domain.LegacyApplicationExtraAnswer;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.application.service.LegacyApplicationExtraAnswerFormatter;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class LegacyResearchExportService {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TXT_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final int FILE_NAME_TITLE_LIMIT = 90;
    private static final String INTRODUCER_LABEL = "소개자 하진혁";
    private static final String LEGACY_RESEARCH_TXT_COLUMNS = "성명/성별/생년월일/나이(만)/직업/회사 학교/휴대폰/유선전화/이메일/주소/추가기재사항";
    private static final int EXCEL_WIDTH_UNIT = 256;
    private static final int EXCEL_MAX_COLUMN_WIDTH = 80 * EXCEL_WIDTH_UNIT;
    private static final int[] LEGACY_COLUMN_MIN_WIDTHS = {
            8, 14, 8, 12, 8, 14, 18, 18, 16, 10, 28, 45, 12, 28, 16
    };
    private static final List<ColumnDefinition> LEGACY_COLUMNS = List.of(
            new ColumnDefinition("No", row -> stringValue(row.sequence())),
            new ColumnDefinition("성명", ExportRow::applicantName),
            new ColumnDefinition("성별", ExportRow::genderCode),
            new ColumnDefinition("생년월일", row -> stringValue(row.birthDate())),
            new ColumnDefinition("나이", ExportRow::ageText),
            new ColumnDefinition("직업", ExportRow::jobText),
            new ColumnDefinition("회사/학교", ExportRow::organizationText),
            new ColumnDefinition("휴대폰", ExportRow::mobilePhone),
            new ColumnDefinition("전화번호", ExportRow::telPhone),
            new ColumnDefinition("지역", ExportRow::regionText),
            new ColumnDefinition("주소", ExportRow::address),
            new ColumnDefinition("추가기재사항", ExportRow::extraComment),
            new ColumnDefinition("참석", ExportRow::priorResearchText),
            new ColumnDefinition("이메일", ExportRow::emailAddress),
            new ColumnDefinition("블랙리스트 여부", row -> yesNoLabel(row.isBlacklisted()))
    );

    private final ResearchApplicationMapper researchApplicationMapper;
    private final LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    private final LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    private final ResearchMasterService researchMasterService;
    private final AdminExportLogMapper adminExportLogMapper;
    private final AdminActionLogService adminActionLogService;

    public LegacyResearchExportService(
            ResearchApplicationMapper researchApplicationMapper,
            LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper,
            LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper,
            ResearchMasterService researchMasterService,
            AdminExportLogMapper adminExportLogMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.researchApplicationMapper = researchApplicationMapper;
        this.legacyApplicationExtraAnswerMapper = legacyApplicationExtraAnswerMapper;
        this.legacyApplicationSearchIndexMapper = legacyApplicationSearchIndexMapper;
        this.researchMasterService = researchMasterService;
        this.adminExportLogMapper = adminExportLogMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public ExportPayload prepareXlsx(Long researchNo, List<Long> researchAppSeqs) {
        ExportContext context = buildContext(researchNo, researchAppSeqs);
        byte[] content = buildXlsx(context);
        String fileName = buildApplicationFileName(context.researchTitle(), context.rows().size(), "xlsx");
        return new ExportPayload(fileName, XLSX_CONTENT_TYPE, content, context.rows().size());
    }

    public ExportPayload prepareProvideXlsx(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        List<ResearchApplication> applications = researchApplicationMapper.findUnprovidedByResearchNo(researchNo);
        ExportContext context = buildContext(researchMaster, applications);
        byte[] content = buildXlsx(context);
        String fileName = buildApplicationFileName(context.researchTitle(), context.rows().size(), "xlsx");
        return new ExportPayload(fileName, XLSX_CONTENT_TYPE, content, context.rows().size());
    }

    public ExportPayload prepareTxt(Long researchNo, List<Long> researchAppSeqs) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        List<ResearchApplication> applications = findApplications(researchNo, researchAppSeqs);
        byte[] content = buildTxt(researchMaster, applications, "전체 대상");
        String fileName = buildApplicationFileName(researchMaster.getResearchTitle(), applications.size(), "txt");
        return new ExportPayload(fileName, TXT_CONTENT_TYPE, content, applications.size());
    }

    public ExportPayload prepareProvideTxt(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        List<ResearchApplication> applications = researchApplicationMapper.findUnprovidedByResearchNo(researchNo);
        byte[] content = buildTxt(researchMaster, applications, "정보 제공 대상");
        String fileName = buildApplicationFileName(researchMaster.getResearchTitle(), applications.size(), "txt");
        return new ExportPayload(fileName, TXT_CONTENT_TYPE, content, applications.size());
    }

    public ExportFileDescriptor describeXlsx(Long researchNo) {
        ExportContext context = buildContext(researchNo, null);
        return new ExportFileDescriptor(buildApplicationFileName(context.researchTitle(), context.rows().size(), "xlsx"), XLSX_CONTENT_TYPE);
    }

    public ExportFileDescriptor describeProvideXlsx(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        int count = researchApplicationMapper.findUnprovidedByResearchNo(researchNo).size();
        return new ExportFileDescriptor(buildApplicationFileName(researchMaster.getResearchTitle(), count, "xlsx"), XLSX_CONTENT_TYPE);
    }

    public ExportFileDescriptor describeTxt(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        int count = researchApplicationMapper.findAllByResearchNo(researchNo).size();
        return new ExportFileDescriptor(buildApplicationFileName(researchMaster.getResearchTitle(), count, "txt"), TXT_CONTENT_TYPE);
    }

    public ExportFileDescriptor describeProvideTxt(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        int count = researchApplicationMapper.findUnprovidedByResearchNo(researchNo).size();
        return new ExportFileDescriptor(buildApplicationFileName(researchMaster.getResearchTitle(), count, "txt"), TXT_CONTENT_TYPE);
    }

    @Transactional("adminTransactionManager")
    public void streamXlsx(
            Long researchNo,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        ExportPayload payload = prepareXlsx(researchNo, null);
        writePayload(outputStream, payload);
        writeLogs(researchNo, "LEGACY_RESEARCH_XLSX", fileName, payload.exportedCount(), principal, request);
    }

    @Transactional("adminTransactionManager")
    public void streamProvideXlsx(
            Long researchNo,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        ExportPayload payload = prepareProvideXlsx(researchNo);
        writePayload(outputStream, payload);
        writeLogs(researchNo, "LEGACY_RESEARCH_PROVIDE_XLSX", fileName, payload.exportedCount(), principal, request);
    }

    @Transactional("adminTransactionManager")
    public void streamTxt(
            Long researchNo,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        ExportPayload payload = prepareTxt(researchNo, null);
        writePayload(outputStream, payload);
        writeLogs(researchNo, "LEGACY_RESEARCH_TXT", fileName, payload.exportedCount(), principal, request);
    }

    @Transactional("adminTransactionManager")
    public void streamProvideTxt(
            Long researchNo,
            String fileName,
            AdminPrincipal principal,
            HttpServletRequest request,
            OutputStream outputStream
    ) {
        ExportPayload payload = prepareProvideTxt(researchNo);
        writePayload(outputStream, payload);
        writeLogs(researchNo, "LEGACY_RESEARCH_PROVIDE_TXT", fileName, payload.exportedCount(), principal, request);
    }

    private ExportContext buildContext(Long researchNo, List<Long> researchAppSeqs) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        List<ResearchApplication> applications = findApplications(researchNo, researchAppSeqs);
        return buildContext(researchMaster, applications);
    }

    private ExportContext buildContext(ResearchMaster researchMaster, List<ResearchApplication> applications) {
        applications = enrichEmails(researchMaster.getResearchNo(), applications);
        Map<Long, String> extraCommentBySeq = formattedExtraCommentBySeq(researchMaster);
        List<LegacyApplicationExtraAnswer> extraAnswers = extraAnswers(researchMaster.getResearchNo());
        List<ExtraQuestion> extraQuestions = extraQuestions(extraAnswers, singleAdditionalGroup(researchMaster));
        Map<Long, Map<String, String>> dynamicAnswersBySeq = dynamicAnswersBySeq(extraAnswers, extraQuestions);
        List<ExportRow> rows = new ArrayList<>();
        for (int index = 0; index < applications.size(); index++) {
            rows.add(toRow(index + 1, applications.get(index), extraCommentBySeq, dynamicAnswersBySeq));
        }
        return new ExportContext(researchMaster.getResearchTitle(), rows, extraQuestions);
    }

    private List<ResearchApplication> findApplications(Long researchNo, List<Long> researchAppSeqs) {
        return researchAppSeqs == null || researchAppSeqs.isEmpty()
                ? enrichEmails(researchNo, researchApplicationMapper.findAllByResearchNo(researchNo))
                : enrichEmails(researchNo, researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(researchNo, researchAppSeqs));
    }

    private ExportRow toRow(
            int sequence,
            ResearchApplication application,
            Map<Long, String> extraCommentBySeq,
            Map<Long, Map<String, String>> dynamicAnswersBySeq
    ) {
        return new ExportRow(
                sequence,
                application.getAppName(),
                legacySexLabel(application.getAppSex()),
                application.getAppBirth(),
                application.getAppAge(),
                application.getAppJob(),
                application.getAppCompany(),
                application.getAppHphoneLabel(),
                application.getAppTeleLabel(),
                "",
                application.getAppAddr(),
                extraCommentBySeq.getOrDefault(application.getResearchAppSeq(), application.getAddComment()),
                "",
                application.getAppEmail(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                application.getProvideYn(),
                "",
                legacyDateTimeLabel(application.getRegistDt()),
                dynamicAnswersBySeq.getOrDefault(application.getResearchAppSeq(), Map.of())
        );
    }

    private List<ResearchApplication> enrichEmails(Long researchNo, List<ResearchApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return applications;
        }
        List<ResearchApplication> indexedApplications;
        try {
            indexedApplications = legacyApplicationSearchIndexMapper.findByResearchNo(researchNo);
        } catch (RuntimeException ignored) {
            indexedApplications = List.of();
        }
        if (indexedApplications == null || indexedApplications.isEmpty()) {
            return applications;
        }
        Map<Long, String> emailBySeq = indexedApplications.stream()
                .filter(application -> application.getResearchAppSeq() != null)
                .collect(java.util.stream.Collectors.toMap(
                        ResearchApplication::getResearchAppSeq,
                        application -> stringValue(application.getAppEmail()),
                        (left, right) -> left
                ));
        for (ResearchApplication application : applications) {
            if (application.getResearchAppSeq() != null) {
                application.setAppEmail(emailBySeq.get(application.getResearchAppSeq()));
            }
        }
        return applications;
    }

    private byte[] buildXlsx(ExportContext context) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Applications");
            CellStyle headerStyle = createExportCellStyle(workbook, true);
            CellStyle bodyStyle = createExportCellStyle(workbook, false);
            List<ColumnDefinition> columns = exportColumns(context.extraQuestions());
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < columns.size(); index++) {
                var cell = headerRow.createCell(index);
                cell.setCellValue(columns.get(index).header());
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (ExportRow exportRow : context.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    var cell = row.createCell(columnIndex);
                    cell.setCellValue(sanitizeSpreadsheetValue(columns.get(columnIndex).value(exportRow)));
                    cell.setCellStyle(bodyStyle);
                }
            }
            for (int index = 0; index < columns.size(); index++) {
                sheet.autoSizeColumn(index);
                applyReadableColumnWidth(sheet, index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("XLSX 파일을 생성하지 못했습니다.", ex);
        }
    }

    private void applyReadableColumnWidth(Sheet sheet, int columnIndex) {
        int minWidth = columnIndex < LEGACY_COLUMN_MIN_WIDTHS.length
                ? LEGACY_COLUMN_MIN_WIDTHS[columnIndex] * EXCEL_WIDTH_UNIT
                : 16 * EXCEL_WIDTH_UNIT;
        sheet.setColumnWidth(columnIndex, Math.min(Math.max(sheet.getColumnWidth(columnIndex), minWidth), EXCEL_MAX_COLUMN_WIDTH));
    }

    private List<ColumnDefinition> exportColumns(List<ExtraQuestion> extraQuestions) {
        List<ColumnDefinition> columns = new ArrayList<>(LEGACY_COLUMNS);
        int extraColumnIndex = 11;
        if (columns.size() > extraColumnIndex) {
            columns.remove(extraColumnIndex);
        }
        for (int index = 0; index < extraQuestions.size(); index++) {
            ExtraQuestion question = extraQuestions.get(index);
            columns.add(extraColumnIndex + index, new ColumnDefinition(question.header(), row -> row.dynamicAnswers().get(question.key())));
        }
        return columns;
    }

    private byte[] buildTxt(ResearchMaster researchMaster, List<ResearchApplication> applications, String titleSuffix) {
        applications = enrichEmails(researchMaster.getResearchNo(), applications);
        Map<Long, String> extraCommentBySeq = formattedExtraCommentBySeq(researchMaster);
        StringBuilder builder = new StringBuilder();
        builder.append(sanitizeTxt(researchMaster.getResearchTitle())).append(" - ").append(titleSuffix).append(System.lineSeparator());
        builder.append(applications.size()).append("건").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append(LEGACY_RESEARCH_TXT_COLUMNS).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        for (int index = 0; index < applications.size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(sanitizeTxt(providePreviewLine(applications.get(index), extraCommentBySeq)))
                    .append(System.lineSeparator());
            if (index + 1 < applications.size()) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String providePreviewLine(ResearchApplication application, Map<Long, String> extraCommentBySeq) {
        String original = application.getAddComment();
        application.setAddComment(extraCommentBySeq.getOrDefault(application.getResearchAppSeq(), compactAdditionalComment(original)));
        try {
            return application.getProvidePreviewLine();
        } finally {
            application.setAddComment(original);
        }
    }

    private CellStyle createExportCellStyle(XSSFWorkbook workbook, boolean header) {
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) (header ? 10 : 10));
        font.setBold(header);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private Map<Long, String> formattedExtraCommentBySeq(ResearchMaster researchMaster) {
        String fallbackGroup = singleAdditionalGroup(researchMaster);
        try {
            return legacyApplicationExtraAnswerMapper.findByResearchNo(researchMaster.getResearchNo()).stream()
                    .filter(answer -> answer.getResearchAppSeq() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            LegacyApplicationExtraAnswer::getResearchAppSeq,
                            java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.collectingAndThen(
                                    java.util.stream.Collectors.toList(),
                                    answers -> LegacyApplicationExtraAnswerFormatter.formatInlineSlash(answers, fallbackGroup)
                            )
                    ));
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private List<LegacyApplicationExtraAnswer> extraAnswers(Long researchNo) {
        try {
            return legacyApplicationExtraAnswerMapper.findByResearchNo(researchNo);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<ExtraQuestion> extraQuestions(List<LegacyApplicationExtraAnswer> answers, String fallbackGroup) {
        Map<String, ExtraQuestion> questions = new java.util.LinkedHashMap<>();
        String inferredGroup = null;
        for (LegacyApplicationExtraAnswer answer : answers) {
            String label = trimToNull(answer.getQuestionLabel());
            if (label == null || LegacyApplicationExtraAnswerFormatter.GROUP_MARKER_LABEL.equals(label)) {
                continue;
            }
            if (label.startsWith("*")) {
                inferredGroup = label.replaceFirst("^\\*+\\s*", "").trim();
                continue;
            }
            String group = trimToNull(answer.getQuestionGroup());
            if (group == null) {
                group = inferredGroup == null ? fallbackGroup : inferredGroup;
            }
            String key = extraQuestionKey(group, label);
            questions.putIfAbsent(key, new ExtraQuestion(key, extraQuestionHeader(group, label)));
        }
        return List.copyOf(questions.values());
    }

    private Map<Long, Map<String, String>> dynamicAnswersBySeq(List<LegacyApplicationExtraAnswer> answers, List<ExtraQuestion> questions) {
        if (answers.isEmpty() || questions.isEmpty()) {
            return Map.of();
        }
        java.util.Set<String> questionKeys = questions.stream()
                .map(ExtraQuestion::key)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<Long, Map<String, String>> result = new java.util.LinkedHashMap<>();
        String inferredGroup = null;
        for (LegacyApplicationExtraAnswer answer : answers) {
            if (answer.getResearchAppSeq() == null) {
                continue;
            }
            String label = trimToNull(answer.getQuestionLabel());
            if (label == null || LegacyApplicationExtraAnswerFormatter.GROUP_MARKER_LABEL.equals(label)) {
                continue;
            }
            if (label.startsWith("*")) {
                inferredGroup = label.replaceFirst("^\\*+\\s*", "").trim();
                continue;
            }
            String group = trimToNull(answer.getQuestionGroup());
            if (group == null) {
                group = inferredGroup;
            }
            String key = extraQuestionKey(group, label);
            if (!questionKeys.contains(key)) {
                continue;
            }
            result.computeIfAbsent(answer.getResearchAppSeq(), ignored -> new java.util.LinkedHashMap<>())
                    .put(key, stringValue(answer.getAnswerText()));
        }
        return result;
    }

    private String extraQuestionKey(String group, String label) {
        return stringValue(group).trim() + "\n" + stringValue(label).trim();
    }

    private String extraQuestionHeader(String group, String label) {
        String normalizedGroup = trimToNull(group);
        return normalizedGroup == null ? label : "[" + normalizedGroup + "] " + label;
    }

    private String singleAdditionalGroup(ResearchMaster researchMaster) {
        if (researchMaster == null) {
            return null;
        }
        List<String> groups = ApplicationFormNoticeParser.parseDetails(researchMaster.getAddComment()).stream()
                .map(ApplicationFormNoticeItem::groupLabel)
                .filter(group -> group != null && !group.isBlank())
                .distinct()
                .toList();
        return groups.size() == 1 ? groups.get(0) : null;
    }

    private void writeLogs(
            Long researchNo,
            String exportType,
            String fileName,
            int exportedCount,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        AdminExportLog exportLog = new AdminExportLog();
        exportLog.setResearchNo(researchNo);
        exportLog.setExportType(exportType);
        exportLog.setFileName(fileName);
        exportLog.setExportedCount(exportedCount);
        adminExportLogMapper.insert(exportLog);

        adminActionLogService.log(
                principal == null ? null : principal.getId(),
                "APPLICATION_EXPORT",
                "RESEARCH",
                String.valueOf(researchNo),
                exportTypeLabel(exportType) + ": 신청자 " + exportedCount + "건",
                request
        );
    }

    private String exportTypeLabel(String exportType) {
        return switch (exportType) {
            case "LEGACY_RESEARCH_XLSX" -> "전체 신청자 엑셀 내보내기";
            case "LEGACY_RESEARCH_TXT" -> "전체 신청자 텍스트 내보내기";
            case "LEGACY_RESEARCH_PROVIDE_XLSX" -> "정보 제공 대상 엑셀 내보내기";
            case "LEGACY_RESEARCH_PROVIDE_TXT" -> "정보 제공 대상 텍스트 내보내기";
            default -> exportType;
        };
    }

    private void writePayload(OutputStream outputStream, ExportPayload payload) {
        try {
            outputStream.write(payload.content());
            outputStream.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("XLSX 파일을 생성하지 못했습니다.", ex);
        }
    }

    private String buildApplicationFileName(String title, int applicantCount, String extension) {
        return sanitizeFileNamePart(title)
                + " "
                + applicantCount
                + "명 "
                + INTRODUCER_LABEL
                + "."
                + extension;
    }

    private String sanitizeFileNamePart(String value) {
        String sanitized = value == null ? "" : value;
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        if (sanitized.isBlank()) {
            sanitized = "applications";
        }
        if (sanitized.length() > FILE_NAME_TITLE_LIMIT) {
            sanitized = sanitized.substring(0, FILE_NAME_TITLE_LIMIT).trim();
        }
        return sanitized;
    }

    private String sanitizeTxt(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private String compactAdditionalComment(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r\n", " / ")
                .replace('\n', '/')
                .replace('\r', '/')
                .replaceAll("\\s*/\\s*", " / ")
                .replaceAll("(\\s*/\\s*)+", " / ")
                .trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String legacySexLabel(String value) {
        if ("1".equals(value)) {
            return "남자";
        }
        if ("2".equals(value)) {
            return "여자";
        }
        return stringValue(value);
    }

    private static LocalDateTime legacyDateTimeLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() >= 14) {
            return LocalDateTime.of(
                    Integer.parseInt(digits.substring(0, 4)),
                    Integer.parseInt(digits.substring(4, 6)),
                    Integer.parseInt(digits.substring(6, 8)),
                    Integer.parseInt(digits.substring(8, 10)),
                    Integer.parseInt(digits.substring(10, 12)),
                    Integer.parseInt(digits.substring(12, 14))
            );
        }
        if (digits.length() >= 8) {
            return LocalDateTime.of(
                    Integer.parseInt(digits.substring(0, 4)),
                    Integer.parseInt(digits.substring(4, 6)),
                    Integer.parseInt(digits.substring(6, 8)),
                    0,
                    0
            );
        }
        return null;
    }

    private static String yesNoLabel(String value) {
        if ("Y".equalsIgnoreCase(value)) {
            return "예";
        }
        if ("N".equalsIgnoreCase(value)) {
            return "아니오";
        }
        return stringValue(value);
    }

    private static String applicationStatusLabel(String value) {
        return stringValue(value);
    }

    private static String deliveryStatusLabel(String value) {
        return stringValue(value);
    }

    private static String blacklistModeLabel(String value) {
        return stringValue(value);
    }

    private record ColumnDefinition(String header, Function<ExportRow, String> extractor) {
        String value(ExportRow row) {
            String value = extractor.apply(row);
            return value == null ? "" : value;
        }
    }

    private record ExportContext(String researchTitle, List<ExportRow> rows, List<ExtraQuestion> extraQuestions) {
    }

    private record ExtraQuestion(String key, String header) {
    }

    private record ExportRow(
            int sequence,
            String applicantName,
            String genderCode,
            Object birthDate,
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
            Object appliedAt,
            Map<String, String> dynamicAnswers
    ) {
    }
}
