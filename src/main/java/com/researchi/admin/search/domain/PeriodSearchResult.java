package com.researchi.admin.search.domain;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.notification.domain.AdminNotificationLog;

import java.util.List;

public record PeriodSearchResult(
        String scope,
        String dateFieldLabel,
        int resultCount,
        Long sendDocumentSrl,
        List<ApplicationRecord> applications,
        List<AdminMailSendJob> mailJobs,
        List<ActionLogItem> actionLogs,
        List<AdminNotificationLog> notificationLogs
) {
}
