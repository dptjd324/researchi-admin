package com.researchi.admin.log.service;

import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.domain.SearchLogItem;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminLogService {

    private final AdminActionLogMapper adminActionLogMapper;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminSearchLogMapper adminSearchLogMapper;
    private final AdminNotificationLogMapper adminNotificationLogMapper;

    public AdminLogService(
            AdminActionLogMapper adminActionLogMapper,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminSearchLogMapper adminSearchLogMapper,
            AdminNotificationLogMapper adminNotificationLogMapper
    ) {
        this.adminActionLogMapper = adminActionLogMapper;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminSearchLogMapper = adminSearchLogMapper;
        this.adminNotificationLogMapper = adminNotificationLogMapper;
    }

    public List<ActionLogItem> getActionLogs() {
        return adminActionLogMapper.findAll();
    }

    public long countActionLogs() {
        return adminActionLogMapper.countAll();
    }

    public List<ActionLogItem> getActionLogsPage(int limit, int offset) {
        return adminActionLogMapper.findPage(limit, offset);
    }

    public List<AdminMailSendJob> getMailLogs() {
        return addJobTitles(adminMailSendJobMapper.findAll());
    }

    public long countMailLogs() {
        return adminMailSendJobMapper.countAll();
    }

    public List<AdminMailSendJob> getMailLogsPage(int limit, int offset) {
        return addJobTitles(adminMailSendJobMapper.findPage(limit, offset));
    }

    public List<SearchLogItem> getSearchLogs() {
        return adminSearchLogMapper.findAll();
    }

    public long countSearchLogs() {
        return adminSearchLogMapper.countAll();
    }

    public List<SearchLogItem> getSearchLogsPage(int limit, int offset) {
        return adminSearchLogMapper.findPage(limit, offset);
    }

    public List<AdminNotificationLog> getNotificationLogs() {
        return adminNotificationLogMapper.findAll();
    }

    public long countNotificationLogs() {
        return adminNotificationLogMapper.countAll();
    }

    public List<AdminNotificationLog> getNotificationLogsPage(int limit, int offset) {
        return adminNotificationLogMapper.findPage(limit, offset);
    }

    private List<AdminMailSendJob> addJobTitles(List<AdminMailSendJob> mailJobs) {
        return mailJobs.stream()
                .peek(job -> job.setJobTitle(mailLogTitle(job)))
                .toList();
    }

    private String mailLogTitle(AdminMailSendJob job) {
        if (job.getMailSubjectSnapshot() != null && !job.getMailSubjectSnapshot().isBlank()) {
            return job.getMailSubjectSnapshot();
        }
        return "CODE " + job.getDocumentSrl();
    }

    public StatusBarSummary getStatusBarSummary() {
        return new StatusBarSummary(
                adminActionLogMapper.countAll(),
                adminMailSendJobMapper.countAll(),
                adminSearchLogMapper.countAll(),
                adminNotificationLogMapper.countAll(),
                adminActionLogMapper.findLatestCreatedAt(),
                adminMailSendJobMapper.findLatestActivityAt(),
                adminSearchLogMapper.findLatestSearchedAt(),
                adminNotificationLogMapper.findLatestCreatedAt()
        );
    }
}
