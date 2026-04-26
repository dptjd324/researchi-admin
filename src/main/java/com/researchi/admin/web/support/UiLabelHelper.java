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
            case "N" -> "아니오";
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
            case "MANUAL_REVIEW" -> "수동 검토";
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
            case "SMS" -> "문자";
            case "" -> "-";
            default -> value;
        };
    }

    public String triggerType(String value) {
        return switch (normalize(value)) {
            case "MANUAL" -> "수동발송";
            case "THRESHOLD" -> "임계치발송";
            case "SCHEDULED" -> "예약 발송";
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
            case "" -> "-";
            default -> value;
        };
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
