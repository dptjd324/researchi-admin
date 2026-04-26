package com.researchi.admin.export.domain;

public record ExportPayload(
        String fileName,
        String contentType,
        byte[] content,
        int exportedCount
) {
}
