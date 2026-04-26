package com.researchi.admin.job.support;

import java.util.List;

public record ApplicationFormNoticeItem(
        String label,
        String type,
        List<ApplicationFormNoticeOption> options
) {
}
