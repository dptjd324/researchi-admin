package com.researchi.admin.legacy.publish.domain;

public class ManualPublishContent {

    private final String title;
    private final String body;
    private final String bodyHtml;

    public ManualPublishContent(String title, String body) {
        this(title, body, "");
    }

    public ManualPublishContent(String title, String body, String bodyHtml) {
        this.title = title;
        this.body = body;
        this.bodyHtml = bodyHtml;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }
}
