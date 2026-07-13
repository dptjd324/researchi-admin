package com.researchi.admin.web.support;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TextLinkRenderer {

    private static final Pattern LINK_LINE_PATTERN = Pattern.compile("^\\s*(.+?)\\s+<((?i:https?://)[^\\s<>]+)>\\s*$");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]*)]\\(((?i:https?://)[^\\s)]+)\\)");
    private static final Pattern ANGLE_URL_PATTERN = Pattern.compile("<((?i:https?://)[^\\s<>\\]]+)>");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("\\[(size|color)=(4|8|12|14|16|18|20|24|28|32|black|blue|red|green|yellow)\\](.*?)\\[/\\1\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\[bold](.*?)\\[/bold\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALIGN_PATTERN = Pattern.compile("^\\[align=(left|center|right)](.*)\\[/align]$");
    private static final Pattern FORMAT_CLOSERS_PATTERN = Pattern.compile("^(?:\\s|\\[/size]|\\[/color]|\\[/bold]|\\[/align])*$");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("</?[A-Za-z][A-Za-z0-9:-]*(?:\\s[^<>]*)?/?>");

    public String render(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder rendered = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                rendered.append("<br>");
            }
            rendered.append(renderLine(lines[i]));
        }
        return rendered.toString();
    }

    public String renderWithDefaultApplyButton(String text, String applyUrl) {
        String rendered = render(text);
        return appendDefaultApplyButton(rendered, applyUrl);
    }

    public String renderCompactWithDefaultApplyButton(String text, String applyUrl) {
        String rendered = renderCompact(text);
        return appendDefaultApplyButton(rendered, applyUrl);
    }

    private String appendDefaultApplyButton(String rendered, String applyUrl) {
        if (applyUrl == null || applyUrl.isBlank() || !isAllowedUrl(applyUrl)) {
            return rendered;
        }
        String button = "<div style=\"text-align:center; margin-top:24px;\">"
                + "<a href=\"" + escapeHtml(applyUrl.trim()) + "\" target=\"_blank\" rel=\"noopener noreferrer\" "
                + "style=\"display:inline-block; min-width:94px; padding:7px 18px; "
                + "background:#2698bd; color:#ffffff; font-size:28px; line-height:1.15; "
                + "font-weight:700; text-align:center; text-decoration:none; "
                + "border:1px solid #1e84a6; box-shadow:3px 3px 4px rgba(0,0,0,0.32);\">신청하기</a>"
                + "</div>";
        return rendered.isBlank() ? button : rendered + "<br><br>" + button;
    }

    private String renderCompact(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder rendered = new StringBuilder();
        boolean previousContent = false;
        boolean previousBlank = false;
        for (String line : lines) {
            if (line.isBlank()) {
                if (previousContent && !previousBlank) {
                    rendered.append("<br><br>");
                    previousBlank = true;
                }
                continue;
            }
            if (previousContent && !previousBlank) {
                rendered.append(" ");
            }
            rendered.append(renderLine(line));
            previousContent = true;
            previousBlank = false;
        }
        return rendered.toString();
    }

    private String renderLine(String line) {
        line = stripStoredHtmlTags(line);
        java.util.regex.Matcher alignMatcher = ALIGN_PATTERN.matcher(line);
        if (alignMatcher.matches()) {
            String align = alignMatcher.group(1);
            String content = alignMatcher.group(2);
            return "<div style=\"text-align:" + align + "\">" + renderInline(content) + "</div>";
        }
        return renderInline(line);
    }

    private String renderInline(String line) {
        String renderedLinkLine = renderFormattedLinkLine(line);
        if (renderedLinkLine != null) {
            return renderedLinkLine;
        }

        return renderFormattedSegments(line, false);
    }

    private String renderFormattedSegments(String line, boolean formattingOnly) {
        java.util.regex.Matcher formatMatcher = FORMAT_PATTERN.matcher(line);
        java.util.regex.Matcher boldMatcher = BOLD_PATTERN.matcher(line);
        StringBuilder rendered = new StringBuilder();
        int previous = 0;
        while (true) {
            boolean hasFormat = formatMatcher.find(previous);
            boolean hasBold = boldMatcher.find(previous);
            if (!hasFormat && !hasBold) {
                break;
            }
            boolean useBold = hasBold && (!hasFormat || boldMatcher.start() < formatMatcher.start());
            int start = useBold ? boldMatcher.start() : formatMatcher.start();
            int end = useBold ? boldMatcher.end() : formatMatcher.end();
            rendered.append(renderPlainSegment(line.substring(previous, start), formattingOnly));
            if (useBold) {
                rendered.append("<strong>")
                        .append(renderFormattedSegments(boldMatcher.group(1), formattingOnly))
                        .append("</strong>");
            } else {
                rendered.append("<span style=\"")
                        .append(formatStyle(formatMatcher.group(1).toLowerCase(), formatMatcher.group(2).toLowerCase()))
                        .append("\">")
                        .append(renderFormattedSegments(formatMatcher.group(3), formattingOnly))
                        .append("</span>");
            }
            previous = end;
        }
        rendered.append(renderPlainSegment(line.substring(previous), formattingOnly));
        return rendered.toString();
    }

    private String renderPlainSegment(String line, boolean formattingOnly) {
        String plain = stripFormattingMarkers(line);
        return formattingOnly ? escapeHtml(plain) : renderLinks(plain);
    }

    private String renderFormattedLinkLine(String line) {
        java.util.regex.Matcher urlMatcher = ANGLE_URL_PATTERN.matcher(line);
        if (!urlMatcher.find() || !FORMAT_CLOSERS_PATTERN.matcher(line.substring(urlMatcher.end())).matches()) {
            return null;
        }
        if (ANGLE_URL_PATTERN.matcher(line.substring(0, urlMatcher.start())).find()) {
            return null;
        }
        String label = (line.substring(0, urlMatcher.start()) + line.substring(urlMatcher.end())).trim();
        String url = urlMatcher.group(1).trim();
        return anchor(url, renderFormattingOnly(label.isBlank() ? url : label));
    }

    private String renderFormattingOnly(String line) {
        return renderFormattedSegments(line, true);
    }

    private String stripStoredHtmlTags(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(value).replaceAll("");
    }

    private String stripFormattingMarkers(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return stripStoredHtmlTags(value)
                .replaceAll("\\[align=(?:left|center|right)]", "")
                .replace("[/align]", "")
                .replaceAll("\\[size=(?:4|8|12|14|16|18|20|24|28|32)\\]", "")
                .replace("[/size]", "")
                .replaceAll("\\[color=(?:black|blue|red|green|yellow)\\]", "")
                .replace("[/color]", "")
                .replaceAll("(?i)\\[bold]", "")
                .replace("[/bold]", "");
    }

    private String formatStyle(String type, String value) {
        if ("size".equals(type)) {
            return "font-size:" + value + "px; color: inherit; font-weight: inherit;";
        }
        return "color:" + colorToCss(value) + "; font-weight: inherit;";
    }

    private String colorToCss(String color) {
        return switch (color) {
            case "blue" -> "#0044ff";
            case "red" -> "#ff0010";
            case "green" -> "#245b12";
            case "yellow" -> "#ffd400";
            default -> "#111827";
        };
    }

    private String renderLinks(String line) {
        java.util.regex.Matcher linkLineMatcher = LINK_LINE_PATTERN.matcher(line);
        if (linkLineMatcher.matches()) {
            String label = linkLineMatcher.group(1).trim();
            String url = linkLineMatcher.group(2).trim();
            if (ANGLE_URL_PATTERN.matcher(label).find()) {
                return renderAngleAndMarkdownLinks(line);
            }
            return anchor(url, escapeHtml(label.isBlank() ? url : label));
        }

        return renderAngleAndMarkdownLinks(line);
    }

    private String renderAngleAndMarkdownLinks(String line) {
        String lineWithAngleLinks = renderAngleLinks(line);
        java.util.regex.Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(lineWithAngleLinks);
        StringBuilder rendered = new StringBuilder();
        int previous = 0;
        while (markdownMatcher.find()) {
            rendered.append(lineWithAngleLinks, previous, markdownMatcher.start());
            String label = markdownMatcher.group(1).trim();
            String url = markdownMatcher.group(2).trim();
            rendered.append(anchor(url, escapeHtml(label.isBlank() ? url : label)));
            previous = markdownMatcher.end();
        }
        rendered.append(lineWithAngleLinks.substring(previous));
        return rendered.toString();
    }

    private String renderAngleLinks(String line) {
        java.util.regex.Matcher matcher = ANGLE_URL_PATTERN.matcher(line);
        StringBuilder rendered = new StringBuilder();
        int previous = 0;
        while (matcher.find()) {
            rendered.append(escapeHtml(line.substring(previous, matcher.start())));
            String url = matcher.group(1).trim();
            rendered.append(anchor(url, escapeHtml(url)));
            previous = matcher.end();
        }
        rendered.append(escapeHtml(line.substring(previous)));
        return rendered.toString();
    }

    private String anchor(String url, String labelHtml) {
        String escapedUrl = escapeHtml(url);
        return "<a href=\"" + escapedUrl + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + labelHtml + "</a>";
    }

    private boolean isAllowedUrl(String url) {
        return url != null
                && (url.regionMatches(true, 0, "https://", 0, 8)
                || url.regionMatches(true, 0, "http://", 0, 7));
    }

    private String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
