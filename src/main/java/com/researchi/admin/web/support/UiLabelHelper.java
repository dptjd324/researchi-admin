package com.researchi.admin.web.support;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("uiLabels")
public class UiLabelHelper {

    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH시 mm분");
    private static final DateTimeFormatter COMPACT_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern SCRIPT_OR_STYLE_BLOCK = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern BLOCK_BREAK_TAG = Pattern.compile("(?i)<\\s*(br|/p|/div|/li|/tr|/h[1-6])\\b[^>]*>");
    private static final Pattern HTML_TAG = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?[0-9A-Fa-f]+);");
    private static final Pattern EXPORT_DETAIL = Pattern.compile("^Exported\\s+([A-Z0-9_]+)\\s+applications\\s+\\((\\d+)\\s+rows\\)$");

    public String applicationStatus(String value) {
        return switch (normalize(value)) {
            case "RECEIVED" -> "접수";
            case "REVIEWING" -> "검토중";
            case "APPROVED" -> "승인";
            case "REJECTED" -> "반려";
            case "BLOCKED" -> "차단";
            case "" -> "-";
            default -> value;
        };
    }

    public String deliveryStatus(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "대기";
            case "READY" -> "준비 완료";
            case "SENT" -> "발송 완료";
            case "FAILED" -> "발송 실패";
            case "" -> "-";
            default -> value;
        };
    }

    public String recruitStatus(String value) {
        return switch (normalize(value)) {
            case "RECRUITING" -> "모집중";
            case "WAITING" -> "대기";
            case "CLOSED" -> "마감";
            case "" -> "-";
            default -> value;
        };
    }

    public String yesNo(String value) {
        return switch (normalize(value)) {
            case "Y" -> "예";
            case "N" -> "아니오";
            case "" -> "-";
            default -> value;
        };
    }

    public String jobBoard(String value) {
        return switch (normalize(value)) {
            case "NEW", "NEWJOB", "NEW_JOB" -> "좌담회/설문";
            case "ADDITIONAL", "ADDITIONAL_JOB", "ADDITIONAL_WORK", "ADDITIONALBOARD", "ADDITIONAL_BOARD" -> "추가 일감";
            case "FAST" -> "급진행 신청";
            case "RECRUIT" -> "전국/지역 모집";
            case "SHARING" -> "좌담회 후기";
            case "QUESTION" -> "문의";
            case "" -> "-";
            default -> value;
        };
    }

    public String blacklistMode(String value) {
        return switch (normalize(value)) {
            case "MANUAL_REVIEW" -> "관리자 검토";
            case "TEMPORARY_BLOCK", "TEMPORARY_BLOCKED" -> "임시 차단";
            case "PERMANENT_BLOCK", "PERMANENT_BLOCKED" -> "영구 차단";
            case "" -> "-";
            default -> value;
        };
    }

    public String matchStatus(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "대기";
            case "RUNNING" -> "실행중";
            case "COMPLETED" -> "완료";
            case "FAILED" -> "실패";
            case "" -> "-";
            default -> value;
        };
    }

    public String notifyStatus(String value) {
        return deliveryStatus(value);
    }

    public String channelType(String value) {
        return switch (normalize(value)) {
            case "EMAIL" -> "이메일";
            case "EMAIL_SECONDARY" -> "보조 이메일";
            case "SMS", "LEGACY_SMS" -> "문자";
            case "" -> "-";
            default -> value;
        };
    }

    public String triggerType(String value) {
        return switch (normalize(value)) {
            case "MANUAL", "LEGACY_MANUAL" -> "수동 발송";
            case "THRESHOLD", "LEGACY_THRESHOLD" -> "임계치 발송";
            case "SCHEDULED", "LEGACY_SCHEDULED" -> "예약 발송";
            case "SCHEDULED_DAILY", "LEGACY_SCHEDULED_DAILY" -> "매일 예약 발송";
            case "" -> "-";
            default -> value;
        };
    }

    public String sendStatus(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "대기";
            case "SCHEDULED" -> "예약중";
            case "RUNNING" -> "실행중";
            case "SENT" -> "발송 완료";
            case "FAILED" -> "실패";
            case "CANCELLED" -> "취소";
            case "SKIPPED_DUPLICATE" -> "중복 제외";
            case "NO_TARGETS" -> "발송 대상 없음";
            case "" -> "-";
            default -> value;
        };
    }

    public String actionType(String value) {
        return switch (normalize(value)) {
            case "LOGIN_SUCCESS" -> "로그인 성공";
            case "LOGIN_FAILURE" -> "로그인 실패";
            case "LOGOUT" -> "로그아웃";
            case "PASSWORD_CHANGE" -> "비밀번호 변경";
            case "BLACKLIST_CREATE" -> "블랙리스트 등록";
            case "BLACKLIST_UPDATE" -> "블랙리스트 수정";
            case "BLACKLIST_STATUS_UPDATE" -> "블랙리스트 상태 변경";
            case "BLACKLIST_EXPIRE" -> "블랙리스트 만료";
            case "BLACKLIST_EXPORT" -> "블랙리스트 내보내기";
            case "APPLICATION_EXPORT" -> "신청자 자료 내보내기";
            case "APPLICATION_BLACKLIST_REGISTER" -> "신청자 블랙리스트 등록";
            case "MAIL_TEMPLATE_CREATE" -> "메일 템플릿 생성";
            case "MAIL_TEMPLATE_UPDATE" -> "메일 템플릿 수정";
            case "MAIL_SEND_LEGACY_MANUAL" -> "수동 메일 발송";
            case "MAIL_SEND_LEGACY_SCHEDULE" -> "메일 예약 등록";
            case "MAIL_SEND_LEGACY_SCHEDULED_EXECUTE" -> "예약 메일 실행";
            case "MAIL_SEND_LEGACY_SCHEDULED_POST_PROCESS_FAILED" -> "예약 메일 후처리 실패";
            case "MAIL_SEND_LEGACY_THRESHOLD" -> "임계치 메일 발송";
            case "MAIL_SEND_LEGACY_CANCEL" -> "메일 예약 취소";
            case "MAIL_SEND_CANCEL" -> "메일 예약 취소";
            case "KEYWORD_MATCH_RUN" -> "매칭 실행";
            case "LEGACY_KEYWORD_NOTIFICATION_SMS" -> "매칭 문자 알림 발송";
            case "" -> "-";
            default -> value;
        };
    }

    public String actionTypeToneClass(String value) {
        return switch (normalize(value)) {
            case "JOB_CREATE", "JOB_UPDATE", "JOB_DELETE" -> "action-type-badge--job-change";
            default -> "";
        };
    }

    public String targetType(String value) {
        return switch (normalize(value)) {
            case "ADMIN_USER" -> "관리자";
            case "APPLICATION" -> "신청서";
            case "BLACKLIST" -> "블랙리스트";
            case "JOB" -> "공고";
            case "MAIL_SEND_JOB" -> "메일 발송 작업";
            case "MAIL_TEMPLATE" -> "메일 템플릿";
            case "KEYWORD_MATCH_JOB" -> "키워드 매칭 작업";
            case "RESEARCH" -> "좌담회/설문";
            case "" -> "-";
            default -> value;
        };
    }

    public String searchType(String value) {
        return switch (normalize(value)) {
            case "APPLICATION" -> "신청서";
            case "MAIL" -> "메일";
            case "ACTION" -> "액션 로그";
            case "NOTIFICATION" -> "알림 로그";
            case "" -> "-";
            default -> value;
        };
    }

    public String logDetail(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        Matcher exportMatcher = EXPORT_DETAIL.matcher(value.trim());
        if (exportMatcher.matches()) {
            return exportType(exportMatcher.group(1)) + ": 신청자 " + exportMatcher.group(2) + "건";
        }
        return value
                .replace("Legacy scheduled mail post-process failed after status update", "예약 메일 상태 변경 후 후처리 실패")
                .replace("Legacy scheduled mail job #", "예약 메일 작업 #")
                .replace("Legacy threshold mail job #", "임계치 메일 작업 #")
                .replace("Legacy mail send job #", "수동 메일 작업 #")
                .replace("Recipient email was not found in the old research row.", "구 DB 좌담회/설문 행에서 수신 이메일을 찾을 수 없습니다.")
                .replace(" completed: ", " 처리 결과: ")
                .replace(" failed: ", " 실패: ")
                .replace(" registered", " 등록")
                .replace(" cancelled", " 취소")
                .replace("RECEIVED", "접수")
                .replace("REVIEWING", "검토중")
                .replace("APPROVED", "승인")
                .replace("REJECTED", "반려")
                .replace("BLOCKED", "차단")
                .replace("SCHEDULED", "예약중")
                .replace("RUNNING", "실행중")
                .replace("SENT", "발송 완료")
                .replace("FAILED", "실패")
                .replace("CANCELLED", "취소")
                .replace("MANUAL", "수동")
                .replace("THRESHOLD", "임계치")
                .replace("NO_TARGETS", "대상 없음")
                .replace("PROVIDE_YN=N", "정보 제공 상태가 N인")
                .replace("rows", "건")
                .replace("applications", "신청자")
                .replace("Exported", "내보내기 완료");
    }

    public String failReason(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return logDetail(value)
                .replace("Simulated SMS gateway", "SMS 모의 발송")
                .replace("SMS gateway is not configured", "SMS 발송 설정이 없습니다")
                .replace("SMTP dispatch failed", "SMTP 발송 실패")
                .replace("timeout", "시간 초과");
    }

    public String htmlToText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String text = SCRIPT_OR_STYLE_BLOCK.matcher(value).replaceAll(" ");
        text = BLOCK_BREAK_TAG.matcher(text).replaceAll("\n");
        text = HTML_TAG.matcher(text).replaceAll(" ");
        text = decodeHtmlEntities(text).replace('\u00A0', ' ');
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        text = text.replaceAll(" *\\n+ *", "\n").trim();
        return text.isBlank() ? "-" : text;
    }

    public String searchCondition(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value
                .replace("\"scope\"", "\"검색 범위\"")
                .replace("\"keyword\"", "\"검색어\"")
                .replace("\"status\"", "\"상태\"")
                .replace("\"datePreset\"", "\"날짜 조건\"")
                .replace("\"specificDate\"", "\"특정 날짜\"")
                .replace("\"dateFrom\"", "\"시작일\"")
                .replace("\"dateTo\"", "\"종료일\"");
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY_DATE_TIME);
    }

    public String date(LocalDate value) {
        return value == null ? "-" : value.format(DISPLAY_DATE);
    }

    public String time(LocalTime value) {
        return value == null ? "-" : value.format(DISPLAY_TIME);
    }

    public String xeDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim();
        if (!normalized.matches("\\d{14}")) {
            return normalized;
        }
        try {
            return LocalDateTime.parse(normalized, COMPACT_DATE_TIME).format(DISPLAY_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return normalized;
        }
    }

    public String triggerToneClass(String value) {
        return switch (normalize(value)) {
            case "MANUAL", "LEGACY_MANUAL" -> "trigger-badge--manual";
            case "THRESHOLD", "LEGACY_THRESHOLD" -> "trigger-badge--threshold";
            case "SCHEDULED", "SCHEDULED_DAILY", "LEGACY_SCHEDULED", "LEGACY_SCHEDULED_DAILY" -> "trigger-badge--scheduled";
            default -> "trigger-badge--default";
        };
    }

    public String sendStatusToneClass(String value) {
        return switch (normalize(value)) {
            case "SCHEDULED" -> "send-status-badge--scheduled";
            case "RUNNING" -> "send-status-badge--running";
            case "SENT" -> "send-status-badge--sent";
            case "FAILED" -> "send-status-badge--failed";
            case "CANCELLED" -> "send-status-badge--cancelled";
            default -> "send-status-badge--default";
        };
    }

    private String exportType(String value) {
        return switch (normalize(value)) {
            case "LEGACY_RESEARCH_XLSX" -> "전체 신청자 엑셀 내보내기";
            case "LEGACY_RESEARCH_TXT" -> "전체 신청자 텍스트 내보내기";
            case "LEGACY_RESEARCH_PROVIDE_XLSX" -> "정보 제공 대상 엑셀 내보내기";
            case "LEGACY_RESEARCH_PROVIDE_TXT" -> "정보 제공 대상 텍스트 내보내기";
            default -> value;
        };
    }

    private String decodeHtmlEntities(String value) {
        String text = value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(decodeNumericEntity(matcher.group(1))));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String decodeNumericEntity(String value) {
        try {
            int radix = value.startsWith("x") || value.startsWith("X") ? 16 : 10;
            String digits = radix == 16 ? value.substring(1) : value;
            return new String(Character.toChars(Integer.parseInt(digits, radix)));
        } catch (IllegalArgumentException ex) {
            return "&#" + value + ";";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
