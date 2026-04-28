package com.researchi.admin.web.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("uiLabels")
public class UiLabelHelper {

    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm");

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
            case "READY" -> "준비완료";
            case "SENT" -> "발송완료";
            case "FAILED" -> "발송실패";
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
            case "N" -> "아니요";
            case "" -> "-";
            default -> value;
        };
    }

    public String jobBoard(String value) {
        return switch (normalize(value)) {
            case "NEWJOB" -> "신규 공고";
            case "ADDITIONAL" -> "추가 공고";
            case "" -> "-";
            default -> value;
        };
    }

    public String blacklistMode(String value) {
        return switch (normalize(value)) {
            case "MANUAL_REVIEW" -> "관리자 검토";
            case "TEMPORARY_BLOCK" -> "임시 차단";
            case "PERMANENT_BLOCK" -> "영구 차단";
            case "TEMPORARY_BLOCKED" -> "임시 차단 적용";
            case "PERMANENT_BLOCKED" -> "영구 차단 적용";
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
        return switch (normalize(value)) {
            case "PENDING" -> "대기";
            case "READY" -> "준비완료";
            case "SENT" -> "발송완료";
            case "FAILED" -> "실패";
            case "" -> "-";
            default -> value;
        };
    }

    public String channelType(String value) {
        return switch (normalize(value)) {
            case "EMAIL" -> "이메일";
            case "EMAIL_SECONDARY" -> "보조 이메일";
            case "SMS" -> "문자";
            case "" -> "-";
            default -> value;
        };
    }

    public String triggerType(String value) {
        return switch (normalize(value)) {
            case "MANUAL" -> "수동발송";
            case "THRESHOLD" -> "임계치발송";
            case "SCHEDULED" -> "예약발송";
            case "" -> "-";
            default -> value;
        };
    }

    public String sendStatus(String value) {
        return switch (normalize(value)) {
            case "PENDING" -> "대기";
            case "SCHEDULED" -> "예약중";
            case "RUNNING" -> "실행중";
            case "SENT" -> "발송완료";
            case "FAILED" -> "실패";
            case "CANCELLED" -> "취소";
            case "SKIPPED_DUPLICATE" -> "중복으로 건너뜀";
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
            case "JOB_CREATE" -> "공고 등록";
            case "JOB_UPDATE" -> "공고 수정";
            case "JOB_STATUS_UPDATE" -> "공고 상태 변경";
            case "APPLICATION_STATUS_UPDATE" -> "지원서 상태 변경";
            case "APPLICATION_BLACKLIST_REGISTER" -> "지원자 블랙리스트 등록";
            case "APPLICATION_EXPORT" -> "지원서 내보내기";
            case "BLACKLIST_CREATE" -> "블랙리스트 등록";
            case "BLACKLIST_UPDATE" -> "블랙리스트 수정";
            case "BLACKLIST_STATUS_UPDATE" -> "블랙리스트 상태 변경";
            case "BLACKLIST_EXPIRE" -> "블랙리스트 만료";
            case "BLACKLIST_EXPORT" -> "블랙리스트 내보내기";
            case "FORM_FIELD_CREATE" -> "동적 필드 등록";
            case "FORM_FIELD_UPDATE" -> "동적 필드 수정";
            case "FORM_FIELD_DELETE" -> "동적 필드 삭제";
            case "KEYWORD_MATCH_RUN" -> "키워드 매칭 실행";
            case "KEYWORD_NOTIFICATION_EMAIL" -> "이메일 추천 알림";
            case "KEYWORD_NOTIFICATION_SMS" -> "문자 추천 알림";
            case "MAIL_SEND_MANUAL" -> "수동 메일 발송";
            case "MAIL_SEND_SCHEDULE" -> "메일 예약 등록";
            case "MAIL_SEND_SCHEDULED_EXECUTE" -> "예약 메일 실행";
            case "MAIL_SEND_THRESHOLD" -> "임계치 메일 발송";
            case "MAIL_SEND_CANCEL" -> "메일 예약 취소";
            case "MAIL_TEMPLATE_CREATE" -> "메일 템플릿 등록";
            case "MAIL_TEMPLATE_UPDATE" -> "메일 템플릿 수정";
            case "" -> "-";
            default -> value;
        };
    }

    public String targetType(String value) {
        return switch (normalize(value)) {
            case "ADMIN_USER" -> "관리자";
            case "APPLICATION" -> "지원서";
            case "BLACKLIST" -> "블랙리스트";
            case "JOB" -> "공고";
            case "MAIL_SEND_JOB" -> "메일 발송 작업";
            case "MAIL_TEMPLATE" -> "메일 템플릿";
            case "KEYWORD_MATCH_JOB" -> "키워드 매칭 작업";
            case "" -> "-";
            default -> value;
        };
    }

    public String searchType(String value) {
        return switch (normalize(value)) {
            case "APPLICATION" -> "지원서";
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
        String detail = value;
        detail = detail.replaceAll(
                "Keyword notification email dispatch completed for match job #(\\d+) with (\\d+) sends\\.",
                "키워드 이메일 알림 발송 완료 - 매칭 작업 #$1, 발송 $2건"
        );
        detail = detail.replaceAll(
                "Keyword notification sms dispatch completed for match job #(\\d+) with (\\d+) sends\\.",
                "키워드 문자 알림 발송 완료 - 매칭 작업 #$1, 발송 $2건"
        );
        detail = detail.replaceAll(
                "Keyword notification SMS dispatch completed for match job #(\\d+) with (\\d+) sends\\.",
                "키워드 문자 알림 발송 완료 - 매칭 작업 #$1, 발송 $2건"
        );
        detail = detail.replaceAll(
                "Keyword match job #(\\d+) completed with (\\d+) matches\\.",
                "키워드 매칭 작업 #$1 완료, 매칭 $2건"
        );
        detail = detail.replace("Exported XLSX applications (", "지원서 XLSX 내보내기 완료 (");
        detail = detail.replace("Exported TXT applications (", "지원서 TXT 내보내기 완료 (");
        detail = detail.replace("Exported XLSX blacklist entries (", "블랙리스트 XLSX 내보내기 완료 (");
        detail = detail.replace("Exported TXT blacklist entries (", "블랙리스트 TXT 내보내기 완료 (");
        detail = detail.replace(" rows)", "행)");
        detail = detail.replace("RECEIVED", "접수");
        detail = detail.replace("REVIEWING", "검토중");
        detail = detail.replace("APPROVED", "승인");
        detail = detail.replace("REJECTED", "반려");
        detail = detail.replace("BLOCKED", "차단");
        detail = detail.replace("RECRUITING", "모집중");
        detail = detail.replace("WAITING", "대기");
        detail = detail.replace("CLOSED", "마감");
        detail = detail.replace("SCHEDULED", "예약중");
        detail = detail.replace("RUNNING", "실행중");
        detail = detail.replace("SENT", "발송완료");
        detail = detail.replace("FAILED", "실패");
        detail = detail.replace("CANCELLED", "취소");
        detail = detail.replace("MANUAL", "수동");
        detail = detail.replace("THRESHOLD", "임계치");
        return detail;
    }

    public String failReason(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return logDetail(value)
                .replace("Simulated SMS gateway", "SMS 시뮬레이션 발송")
                .replace("SMS gateway is not configured", "SMS 발송 설정이 없습니다")
                .replace("SMTP dispatch failed", "SMTP 발송 실패")
                .replace("timeout", "시간 초과");
    }

    public String searchCondition(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value
                .replace("\"scope\"", "\"검색 범위\"")
                .replace("\"keyword\"", "\"검색어\"")
                .replace("\"documentSrl\"", "\"공고 번호\"")
                .replace("\"status\"", "\"상태\"")
                .replace("\"datePreset\"", "\"날짜 조건\"")
                .replace("\"specificDate\"", "\"특정 날짜\"")
                .replace("\"dateFrom\"", "\"시작일\"")
                .replace("\"dateTo\"", "\"종료일\"")
                .replace("\"APPLICATION\"", "\"지원서\"")
                .replace("\"MAIL\"", "\"메일\"")
                .replace("\"ACTION\"", "\"액션 로그\"")
                .replace("\"NOTIFICATION\"", "\"알림 로그\"")
                .replace("\"TODAY\"", "\"오늘\"")
                .replace("\"THIS_WEEK\"", "\"이번 주\"")
                .replace("\"SPECIFIC_DAY\"", "\"특정 날짜\"")
                .replace("\"CUSTOM\"", "\"직접 지정\"")
                .replace("\"RECEIVED\"", "\"접수\"")
                .replace("\"REVIEWING\"", "\"검토중\"")
                .replace("\"APPROVED\"", "\"승인\"")
                .replace("\"REJECTED\"", "\"반려\"")
                .replace("\"BLOCKED\"", "\"차단\"")
                .replace("\"SENT\"", "\"발송완료\"")
                .replace("\"FAILED\"", "\"실패\"")
                .replace("\"SCHEDULED\"", "\"예약중\"");
    }

    public String dateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(DISPLAY_DATE_TIME);
    }

    public String triggerToneClass(String value) {
        return switch (normalize(value)) {
            case "MANUAL" -> "trigger-badge--manual";
            case "THRESHOLD" -> "trigger-badge--threshold";
            case "SCHEDULED" -> "trigger-badge--scheduled";
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
