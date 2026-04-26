package com.researchi.admin.blacklist.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.domain.BlacklistEntry;
import com.researchi.admin.export.domain.ExportPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BlacklistExportService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> HEADERS = List.of(
            "ID",
            "이름",
            "생년월일",
            "사유",
            "모드",
            "활성 여부",
            "만료 시각",
            "매칭 수",
            "등록자 ID",
            "등록 시각",
            "수정 시각"
    );

    private final BlacklistService blacklistService;
    private final AdminActionLogService adminActionLogService;

    public BlacklistExportService(
            BlacklistService blacklistService,
            AdminActionLogService adminActionLogService
    ) {
        this.blacklistService = blacklistService;
        this.adminActionLogService = adminActionLogService;
    }

    public ExportPayload exportXlsx(
            String keyword,
            String activeYn,
            String blackMode,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        List<BlacklistEntry> entries = blacklistService.findEntries(keyword, activeYn, blackMode);
        byte[] content = buildXlsx(entries);
        writeLog("XLSX", entries.size(), keyword, activeYn, blackMode, principal, request);
        return new ExportPayload(buildFileName("xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content, entries.size());
    }

    public ExportPayload exportTxt(
            String keyword,
            String activeYn,
            String blackMode,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        List<BlacklistEntry> entries = blacklistService.findEntries(keyword, activeYn, blackMode);
        byte[] content = buildTxt(entries);
        writeLog("TXT", entries.size(), keyword, activeYn, blackMode, principal, request);
        return new ExportPayload(buildFileName("txt"), "text/plain; charset=UTF-8", content, entries.size());
    }

    private byte[] buildXlsx(List<BlacklistEntry> entries) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Blacklist");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                headerRow.createCell(index).setCellValue(HEADERS.get(index));
            }
            int rowIndex = 1;
            for (BlacklistEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row, entry);
            }
            for (int index = 0; index < HEADERS.size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("블랙리스트 XLSX 파일을 생성하지 못했습니다.", ex);
        }
    }

    private byte[] buildTxt(List<BlacklistEntry> entries) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, HEADERS);
        for (BlacklistEntry entry : entries) {
            appendLine(builder, List.of(
                    stringValue(entry.getId()),
                    sanitize(entry.getBlackName()),
                    stringValue(entry.getBlackBirthDate()),
                    sanitize(entry.getBlackReason()),
                    sanitize(labelMode(entry.getBlackMode())),
                    sanitize(labelActive(entry.getActiveYn())),
                    stringValue(entry.getExpiresAt()),
                    stringValue(entry.getMatchCount()),
                    stringValue(entry.getCreatedBy()),
                    stringValue(entry.getCreatedAt()),
                    stringValue(entry.getUpdatedAt())
            ));
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void writeRow(Row row, BlacklistEntry entry) {
        row.createCell(0).setCellValue(stringValue(entry.getId()));
        row.createCell(1).setCellValue(sanitize(entry.getBlackName()));
        row.createCell(2).setCellValue(stringValue(entry.getBlackBirthDate()));
        row.createCell(3).setCellValue(sanitize(entry.getBlackReason()));
        row.createCell(4).setCellValue(sanitize(labelMode(entry.getBlackMode())));
        row.createCell(5).setCellValue(sanitize(labelActive(entry.getActiveYn())));
        row.createCell(6).setCellValue(stringValue(entry.getExpiresAt()));
        row.createCell(7).setCellValue(stringValue(entry.getMatchCount()));
        row.createCell(8).setCellValue(stringValue(entry.getCreatedBy()));
        row.createCell(9).setCellValue(stringValue(entry.getCreatedAt()));
        row.createCell(10).setCellValue(stringValue(entry.getUpdatedAt()));
    }

    private void appendLine(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append('\t');
            }
            builder.append(sanitize(values.get(index)));
        }
        builder.append(System.lineSeparator());
    }

    private void writeLog(
            String exportType,
            int exportedCount,
            String keyword,
            String activeYn,
            String blackMode,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        adminActionLogService.log(
                principal.getId(),
                "BLACKLIST_EXPORT",
                "BLACKLIST",
                buildTargetId(keyword, activeYn, blackMode),
                "Exported " + exportType + " blacklist entries (" + exportedCount + " rows)",
                request
        );
    }

    private String buildTargetId(String keyword, String activeYn, String blackMode) {
        String keywordPart = normalizeFilterValue(keyword);
        String activePart = normalizeFilterValue(activeYn);
        String modePart = normalizeFilterValue(blackMode);
        return "FILTER:" + keywordPart + "|" + activePart + "|" + modePart;
    }

    private String buildFileName(String extension) {
        return "blacklist-" + LocalDateTime.now().format(FILE_TS) + "." + extension;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DT);
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        return String.valueOf(value);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return "ALL";
        }
        String trimmed = sanitize(value).trim();
        return trimmed.isEmpty() ? "ALL" : trimmed;
    }

    private String labelMode(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim().toUpperCase()) {
            case "PERMANENT_BLOCK" -> "영구 차단";
            case "TEMPORARY_BLOCK" -> "기간 차단";
            case "MANUAL_REVIEW" -> "수동 검토";
            default -> value;
        };
    }

    private String labelActive(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim().toUpperCase()) {
            case "Y" -> "활성";
            case "N" -> "비활성";
            default -> value;
        };
    }
}
