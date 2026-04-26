package com.researchi.admin.log.service;

import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.domain.SearchLogItem;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminLogService {

    private final AdminActionLogMapper adminActionLogMapper;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminSearchLogMapper adminSearchLogMapper;
    private final AdminNotificationLogMapper adminNotificationLogMapper;
    private final JobService jobService;

    public AdminLogService(
            AdminActionLogMapper adminActionLogMapper,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminSearchLogMapper adminSearchLogMapper,
            AdminNotificationLogMapper adminNotificationLogMapper,
            JobService jobService
    ) {
        this.adminActionLogMapper = adminActionLogMapper;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminSearchLogMapper = adminSearchLogMapper;
        this.adminNotificationLogMapper = adminNotificationLogMapper;
        this.jobService = jobService;
    }

    public List<ActionLogItem> getActionLogs() {
        return adminActionLogMapper.findAll();
    }

    public List<AdminMailSendJob> getMailLogs() {
        Map<Long, String> titlesByDocumentSrl = new LinkedHashMap<>();
        for (JobListItem job : jobService.getJobs()) {
            titlesByDocumentSrl.put(job.getDocumentSrl(), job.getTitle());
        }
        return adminMailSendJobMapper.findAll().stream()
                .peek(job -> job.setJobTitle(titlesByDocumentSrl.getOrDefault(job.getDocumentSrl(), "Job #" + job.getDocumentSrl())))
                .toList();
    }

    public List<SearchLogItem> getSearchLogs() {
        return adminSearchLogMapper.findAll();
    }

    public List<AdminNotificationLog> getNotificationLogs() {
        return adminNotificationLogMapper.findAll();
    }

    public StatusBarSummary getStatusBarSummary() {
        List<ActionLogItem> actionLogs = adminActionLogMapper.findAll();
        List<AdminMailSendJob> mailLogs = adminMailSendJobMapper.findAll();
        List<SearchLogItem> searchLogs = adminSearchLogMapper.findAll();
        List<AdminNotificationLog> notificationLogs = adminNotificationLogMapper.findAll();

        return new StatusBarSummary(
                actionLogs.size(),
                mailLogs.size(),
                searchLogs.size(),
                notificationLogs.size(),
                firstActionAt(actionLogs),
                firstMailAt(mailLogs),
                firstSearchAt(searchLogs),
                firstNotificationAt(notificationLogs)
        );
    }

    private LocalDateTime firstActionAt(List<ActionLogItem> items) {
        return items.isEmpty() ? null : items.get(0).getCreatedAt();
    }

    private LocalDateTime firstMailAt(List<AdminMailSendJob> items) {
        if (items.isEmpty()) {
            return null;
        }
        AdminMailSendJob item = items.get(0);
        if (item.getSentAt() != null) {
            return item.getSentAt();
        }
        if (item.getScheduledAt() != null) {
            return item.getScheduledAt();
        }
        return item.getCreatedAt();
    }

    private LocalDateTime firstSearchAt(List<SearchLogItem> items) {
        return items.isEmpty() ? null : items.get(0).getSearchedAt();
    }

    private LocalDateTime firstNotificationAt(List<AdminNotificationLog> items) {
        return items.isEmpty() ? null : items.get(0).getCreatedAt();
    }
}
