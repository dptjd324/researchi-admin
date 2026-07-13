package com.researchi.admin.legacy.application.service;

import com.researchi.admin.legacy.application.domain.LegacyApplicationExtraAnswer;

import java.util.List;

public final class LegacyApplicationExtraAnswerFormatter {

    public static final String GROUP_MARKER_LABEL = "__GROUP__";

    private LegacyApplicationExtraAnswerFormatter() {
    }

    public static String format(List<LegacyApplicationExtraAnswer> answers) {
        return format(answers, null);
    }

    public static String format(List<LegacyApplicationExtraAnswer> answers, String fallbackGroup) {
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String currentGroup = null;
        String inferredGroup = null;
        boolean hasContent = false;
        for (LegacyApplicationExtraAnswer answer : answers) {
            String answerText = trimToNull(answer.getAnswerText());
            if (answerText == null) {
                continue;
            }
            String group = trimToNull(answer.getQuestionGroup());
            String questionLabel = trimToEmpty(answer.getQuestionLabel());
            if (GROUP_MARKER_LABEL.equals(questionLabel)) {
                group = group == null ? answerText : group;
                if (group != null && !group.equals(currentGroup)) {
                    if (hasContent) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append('[').append(group).append(']').append(System.lineSeparator());
                    currentGroup = group;
                }
                continue;
            }
            if (group == null && questionLabel.startsWith("*")) {
                group = questionLabel.replaceFirst("^\\*+\\s*", "").trim();
                inferredGroup = group;
                if (!group.equals(currentGroup)) {
                    if (hasContent) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append('[').append(group).append(']').append(System.lineSeparator());
                    currentGroup = group;
                }
                continue;
            }
            if (group == null) {
                group = inferredGroup == null ? fallbackGroup : inferredGroup;
            }
            if (group == null) {
                group = "추가기재사항";
            }
            if (group != null && !group.equals(currentGroup)) {
                if (hasContent) {
                    builder.append(System.lineSeparator());
                }
                builder.append('[').append(group).append(']').append(System.lineSeparator());
                currentGroup = group;
            }
            builder.append(questionLabel).append(": ").append(answerText).append(System.lineSeparator());
            hasContent = true;
        }
        return builder.toString().trim();
    }

    public static String formatInlineSlash(List<LegacyApplicationExtraAnswer> answers, String fallbackGroup) {
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        List<String> parts = new java.util.ArrayList<>();
        String inferredGroup = null;
        for (LegacyApplicationExtraAnswer answer : answers) {
            String answerText = trimToNull(answer.getAnswerText());
            if (answerText == null) {
                continue;
            }
            String questionLabel = trimToEmpty(answer.getQuestionLabel());
            if (GROUP_MARKER_LABEL.equals(questionLabel)) {
                continue;
            }
            if (questionLabel.startsWith("*")) {
                inferredGroup = questionLabel.replaceFirst("^\\*+\\s*", "").trim();
                continue;
            }
            String group = trimToNull(answer.getQuestionGroup());
            if (group == null) {
                group = inferredGroup == null ? fallbackGroup : inferredGroup;
            }
            String label = trimToNull(questionLabel);
            if (label == null) {
                label = group == null ? "추가기재사항" : group;
            }
            parts.add(label + ": " + answerText);
        }
        return String.join(" / ", parts);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }
}
