package com.researchi.admin.web.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextLinkRendererTest {

    private final TextLinkRenderer renderer = new TextLinkRenderer();

    @Test
    void renderConvertsInsertedTextLinkLineToAnchor() {
        String html = renderer.render("Apply https://example.com/apply?id=1");

        assertThat(html).doesNotContain("<a href=");
    }

    @Test
    void renderConvertsTextWithAngleBracketUrlToAnchor() {
        String html = renderer.render("Apply <https://example.com/apply?id=1>");

        assertThat(html).contains("<a href=\"https://example.com/apply?id=1\"");
        assertThat(html).contains(">Apply</a>");
        assertThat(html).doesNotContain("[");
        assertThat(html).doesNotContain("]");
    }

    @Test
    void renderKeepsPlainUrlsAsText() {
        String html = renderer.render("Open https://example.com/apply.");

        assertThat(html).isEqualTo("Open https://example.com/apply.");
        assertThat(html).doesNotContain("<a href=");
    }

    @Test
    void renderStillSupportsExistingMarkdownLinks() {
        String html = renderer.render("[Apply](https://example.com/apply)");

        assertThat(html).contains("<a href=\"https://example.com/apply\"");
        assertThat(html).contains(">Apply</a>");
    }

    @Test
    void renderEscapesHtmlButKeepsLineBreaks() {
        String html = renderer.render("<script>alert(1)</script>\nhttps://example.com");

        assertThat(html).contains("alert(1)<br>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("<a href=");
    }

    @Test
    void renderStripsStoredHtmlTagsButKeepsAngleLinkSyntax() {
        String html = renderer.render("<span style=\"color:red\">공지</span> <https://example.com/apply>");

        assertThat(html).contains(">공지</a>");
        assertThat(html).contains("<a href=\"https://example.com/apply\"");
        assertThat(html).doesNotContain("&lt;span");
        assertThat(html).doesNotContain("color:red");
    }

    @Test
    void renderSupportsSelectedTextFormattingMarkers() {
        String html = renderer.render("[align=center]제목[/align]\n본문 [size=18]강조[/size]");

        assertThat(html).contains("<div style=\"text-align:center\">제목</div>");
        assertThat(html).contains("<span style=\"font-size:18px; color: inherit; font-weight: inherit;\">강조</span>");
    }

    @Test
    void renderSupportsExpandedFontSizesAndColors() {
        String html = renderer.render("[size=32]큰 글자[/size] [color=red]빨간 글자[/color]");

        assertThat(html).contains("<span style=\"font-size:32px; color: inherit; font-weight: inherit;\">큰 글자</span>");
        assertThat(html).contains("<span style=\"color:#ff0010; font-weight: inherit;\">빨간 글자</span>");
    }

    @Test
    void renderKeepsNestedColorAndFontSizeFormatting() {
        String html = renderer.render("[color=red][size=24]중첩 서식[/size][/color]");

        assertThat(html).contains("color:#ff0010");
        assertThat(html).contains("font-size:24px");
        assertThat(html).contains("중첩 서식");
        assertThat(html).doesNotContain("[size");
        assertThat(html).doesNotContain("[color");
    }

    @Test
    void renderEscapesHtmlInsideFormattingMarkers() {
        String html = renderer.render("[size=20]<script>alert(1)</script>[/size]");

        assertThat(html).contains("alert(1)");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("&lt;script");
    }

    @Test
    void renderDoesNotExposeNestedFormattingMarkers() {
        String html = renderer.render("[align=center][size=18][size=20][align=center]신청하기[/align][/size][/size][/align]");

        assertThat(html).doesNotContain("[align");
        assertThat(html).doesNotContain("[size");
        assertThat(html).contains("신청하기");
    }

    @Test
    void renderSupportsBoldAndGreenFormatting() {
        String html = renderer.render("[bold][color=green]굵은 초록[/color][/bold]");

        assertThat(html).contains("<strong>");
        assertThat(html).contains("color:#245b12");
        assertThat(html).contains("굵은 초록");
        assertThat(html).doesNotContain("[bold]");
        assertThat(html).doesNotContain("[color");
    }
    @Test
    void renderKeepsFormattedLinkLineClickable() {
        String html = renderer.render("[align=center][size=18]신청하기[/size] <http://localhost:8082/research/46433/apply>[/align]");

        assertThat(html).contains("<a href=\"http://localhost:8082/research/46433/apply\"");
        assertThat(html).contains("신청하기");
        assertThat(html).doesNotContain("[align");
        assertThat(html).doesNotContain("[size");
    }
    @Test
    void renderDoesNotIncludeFormattingClosersInLinkUrl() {
        String html = renderer.render("[align=center][size=18]Apply <http://localhost:8082/research/46433/apply>[/size][/align]");

        assertThat(html).contains("<a href=\"http://localhost:8082/research/46433/apply\"");
        assertThat(html).doesNotContain("apply[/size]");
        assertThat(html).contains("font-size:18px");
    }

    @Test
    void renderConvertsMultipleAngleBracketLinks() {
        String html = renderer.render("Apply <https://example.com/apply> Map <https://example.com/map>");

        assertThat(html).contains("<a href=\"https://example.com/apply\"");
        assertThat(html).contains("<a href=\"https://example.com/map\"");
    }

    @Test
    void renderWithDefaultApplyButtonAppendsStyledApplyLinkAtBottom() {
        String html = renderer.renderWithDefaultApplyButton("본문", "https://example.com/apply");

        assertThat(html).contains("본문<br><br><div style=\"text-align:center; margin-top:24px;\">");
        assertThat(html).contains("background:#2698bd; color:#ffffff; font-size:28px");
        assertThat(html).contains(">신청하기</a>");
        assertThat(html).contains("href=\"https://example.com/apply\"");
    }

    @Test
    void renderCompactWithDefaultApplyButtonCollapsesSingleLineBreaksOnly() {
        String html = renderer.renderCompactWithDefaultApplyButton("LineA\nLineB\n\nParagraphB", "https://example.com/apply");

        assertThat(html).contains("LineA LineB<br><br>ParagraphB");
        assertThat(html).doesNotContain("LineA<br>LineB");
        assertThat(html).contains("href=\"https://example.com/apply\"");
    }
}
