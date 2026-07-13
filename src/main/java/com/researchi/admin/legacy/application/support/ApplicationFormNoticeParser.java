package com.researchi.admin.legacy.application.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ApplicationFormNoticeParser {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\r\\n]+|\\s*/\\s*");
    private static final Pattern NUMBER_PREFIX_PATTERN = Pattern.compile("^(?:질문\\s*)?\\d+(?:-\\d+)*[.)]\\s*");
    private static final Pattern CIRCLED_PREFIX_PATTERN = Pattern.compile("^[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]\\s*");
    private static final List<String> ALLOWED_TYPES = List.of("TEXT", "RADIO", "CHECKBOX", "SELECT", "DATE", "NUMBER");

    private ApplicationFormNoticeParser() {
    }

    public static List<String> parseItems(String rawValue) {
        return parseDetails(rawValue).stream()
                .map(ApplicationFormNoticeItem::label)
                .toList();
    }

    public static List<ApplicationFormNoticeItem> parseDetails(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        if (rawValue.contains("[")
                || rawValue.lines().anyMatch(ApplicationFormNoticeParser::isNumberedLine)
                || rawValue.lines().anyMatch(ApplicationFormNoticeParser::isGroupHeadingLine)) {
            return parseStructuredItems(rawValue);
        }
        return parseLegacyItems(rawValue);
    }

    private static List<ApplicationFormNoticeItem> parseLegacyItems(String rawValue) {
        return SPLIT_PATTERN.splitAsStream(rawValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(ApplicationFormNoticeParser::parseItem)
                .toList();
    }

    private static List<ApplicationFormNoticeItem> parseStructuredItems(String rawValue) {
        List<ApplicationFormNoticeItem> items = new ArrayList<>();
        String groupLabel = null;
        QuestionBlock current = null;
        for (String rawLine : rawValue.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]") && line.length() > 2) {
                flushQuestionBlock(items, current);
                current = null;
                groupLabel = line.substring(1, line.length() - 1).trim();
                continue;
            }
            if (isGroupHeadingLine(line)) {
                flushQuestionBlock(items, current);
                current = null;
                groupLabel = stripGroupHeadingPrefix(line);
                continue;
            }
            if (isNumberedLine(line)) {
                flushQuestionBlock(items, current);
                current = new QuestionBlock(stripNumberPrefix(line), groupLabel);
                continue;
            }
            if (current != null) {
                current.detailLines().add(line);
                continue;
            }
            addItems(items, parseLegacyItems(line), groupLabel);
        }
        flushQuestionBlock(items, current);
        return List.copyOf(items);
    }

    private static boolean isNumberedLine(String line) {
        return stripNumberPrefix(line.trim()).length() < line.trim().length();
    }

    private static String stripNumberPrefix(String value) {
        return CIRCLED_PREFIX_PATTERN.matcher(NUMBER_PREFIX_PATTERN.matcher(value).replaceFirst("")).replaceFirst("").trim();
    }

    private static void flushQuestionBlock(List<ApplicationFormNoticeItem> items, QuestionBlock block) {
        if (block == null || block.label().isBlank()) {
            return;
        }
        ApplicationFormNoticeItem item = buildStructuredItem(block);
        items.add(new ApplicationFormNoticeItem(item.label(), item.type(), item.options(), block.groupLabel()));
    }

    private static ApplicationFormNoticeItem buildStructuredItem(QuestionBlock block) {
        String label = block.label().trim();
        List<String> details = block.detailLines().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (shouldUseDetailLinesAsOptions(label, details)) {
            return new ApplicationFormNoticeItem(label, "RADIO", details.stream()
                    .map(ApplicationFormNoticeOption::fromAdminText)
                    .toList());
        }
        List<ApplicationFormNoticeOption> inlineOptions = parseInlineOptions(label);
        if (!inlineOptions.isEmpty()) {
            return new ApplicationFormNoticeItem(label, "RADIO", inlineOptions);
        }
        if (!details.isEmpty()) {
            label = label + " " + String.join(" ", details);
        }
        return parseItem(label);
    }

    private static boolean shouldUseDetailLinesAsOptions(String label, List<String> details) {
        return details.size() >= 2 && (hasChoiceIntent(label) || details.stream().allMatch(ApplicationFormNoticeParser::looksLikeOptionLine));
    }

    private static boolean hasChoiceIntent(String label) {
        String value = label == null ? "" : label.replace(" ", "");
        return value.contains("선택") || value.contains("택1") || value.contains("여부")
                || value.contains("중택") || value.contains("골라") || value.contains("해당");
    }

    private static boolean looksLikeOptionLine(String line) {
        String value = line == null ? "" : line.trim();
        return !value.isBlank()
                && value.length() <= 45
                && !value.endsWith("?")
                && !value.contains("적어주세요")
                && !value.contains("기재")
                && !isNumberedLine(value);
    }

    private static List<ApplicationFormNoticeOption> parseInlineOptions(String label) {
        if (label == null || !hasChoiceIntent(label) || !label.contains("/")) {
            return List.of();
        }
        String optionText = label.replaceFirst("\\s*(중\\s*)?택\\s*1.*$", "");
        List<String> options = SPLIT_PATTERN.splitAsStream(optionText)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (options.size() < 2) {
            return List.of();
        }
        return options.stream()
                .map(ApplicationFormNoticeOption::fromAdminText)
                .toList();
    }

    private static void addItems(List<ApplicationFormNoticeItem> items, List<ApplicationFormNoticeItem> lineItems, String groupLabel) {
        for (ApplicationFormNoticeItem item : lineItems) {
            items.add(new ApplicationFormNoticeItem(item.label(), item.type(), item.options(), groupLabel));
        }
    }

    private record QuestionBlock(String label, String groupLabel, List<String> detailLines) {
        QuestionBlock(String label, String groupLabel) {
            this(label, groupLabel, new ArrayList<>());
        }
    }

    private static boolean isGroupHeadingLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        return trimmed.startsWith("*") && stripGroupHeadingPrefix(trimmed).length() > 0;
    }

    private static String stripGroupHeadingPrefix(String value) {
        return value.replaceFirst("^\\*+\\s*", "").trim();
    }

    public static String serializeItem(String label, String type, List<ApplicationFormNoticeOption> options) {
        String normalizedLabel = label == null ? "" : label.trim();
        String normalizedType = normalizeType(type);
        String optionText = options == null ? "" : options.stream()
                .map(ApplicationFormNoticeOption::displayValue)
                .map(ApplicationFormNoticeParser::escape)
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
        if ("TEXT".equals(normalizedType) && optionText.isBlank()) {
            return escape(normalizedLabel);
        }
        return escape(normalizedLabel) + "|" + normalizedType + "|" + optionText;
    }

    private static ApplicationFormNoticeItem parseItem(String rawItem) {
        List<String> parts = splitEscaped(rawItem, '|');
        String label = unescape(parts.get(0)).trim();
        if (parts.size() == 1) {
            return new ApplicationFormNoticeItem(label, "TEXT", List.of());
        }
        String type = normalizeType(unescape(parts.get(1)));
        List<ApplicationFormNoticeOption> options = parseOptions(parts.size() > 2 ? parts.get(2) : "");
        if (!requiresOptions(type)) {
            options = List.of();
        }
        return new ApplicationFormNoticeItem(label, type, options);
    }

    private static List<ApplicationFormNoticeOption> parseOptions(String rawOptions) {
        if (rawOptions == null || rawOptions.isBlank()) {
            return List.of();
        }
        List<ApplicationFormNoticeOption> options = new ArrayList<>();
        for (String rawOption : splitEscaped(rawOptions, ';')) {
            if (rawOption.isBlank()) {
                continue;
            }
            List<String> parts = splitEscaped(rawOption, '=');
            String value = unescape(parts.get(0)).trim();
            String label = parts.size() > 1 ? unescape(parts.get(1)).trim() : value;
            if (!value.isBlank() && !label.isBlank()) {
                options.add(ApplicationFormNoticeOption.fromAdminText(label));
            }
        }
        return List.copyOf(options);
    }

    private static boolean requiresOptions(String type) {
        return "RADIO".equals(type) || "CHECKBOX".equals(type) || "SELECT".equals(type);
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "TEXT" : type.trim().toUpperCase();
        return ALLOWED_TYPES.contains(normalized) ? normalized : "TEXT";
    }

    private static List<String> splitEscaped(String value, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (escaping) {
                current.append(ch);
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = true;
                continue;
            }
            if (ch == separator) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (escaping) {
            current.append('\\');
        }
        parts.add(current.toString());
        return parts;
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace(";", "\\;")
                .replace("=", "\\=");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (escaping) {
                result.append(ch);
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = true;
                continue;
            }
            result.append(ch);
        }
        if (escaping) {
            result.append('\\');
        }
        return result.toString();
    }
}
