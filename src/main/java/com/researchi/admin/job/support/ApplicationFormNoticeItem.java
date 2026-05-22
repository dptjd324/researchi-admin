package com.researchi.admin.job.support;

import java.util.List;

public record ApplicationFormNoticeItem(
        String label,
        String type,
        List<ApplicationFormNoticeOption> options,
        String groupLabel
) {
    public ApplicationFormNoticeItem(String label, String type, List<ApplicationFormNoticeOption> options) {
        this(label, type, options, null);
    }
}
