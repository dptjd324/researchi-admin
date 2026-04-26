package com.researchi.admin.mailing.domain;

import java.util.List;

public record MailDispatchRequest(
        List<String> recipients,
        String replyTo,
        String subject,
        String body,
        String attachmentFileName,
        String attachmentContentType,
        byte[] attachmentContent
) {
}
