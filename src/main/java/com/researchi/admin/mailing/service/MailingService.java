package com.researchi.admin.mailing.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportApplicationSource;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportQueryMapper;
import com.researchi.admin.export.service.ExportService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailingHistoryItem;
import com.researchi.admin.mailing.domain.MailingPreview;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.mailing.mapper.AdminMailingApplicationMapper;
import com.researchi.admin.mailing.web.MailScheduleForm;
import com.researchi.admin.mailing.web.MailSendManualForm;
import com.researchi.admin.mailing.web.MailThresholdSettingsForm;
import com.researchi.admin.mailing.web.MailThresholdTriggerForm;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MailingService {

    private static final DateTimeFormatter MAIL_DT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    private static final String DEFAULT_ATTACHMENT_TYPE = "XLSX";
    private static final String DEFAULT_DIRECT_SUBJECT = "공고 지원서 안내";
    private static final String DEFAULT_DIRECT_BODY = "지원서를 첨부해 드립니다.";

    private final AdminMailTemplateMapper adminMailTemplateMapper;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminMailSendTargetMapper adminMailSendTargetMapper;
    private final AdminMailingApplicationMapper adminMailingApplicationMapper;
    private final AdminJobMetaMapper adminJobMetaMapper;
    private final AdminExportQueryMapper adminExportQueryMapper;
    private final ExportService exportService;
    private final JobService jobService;
    private final ClientService clientService;
    private final PublicFormProtectionService protectionService;
    private final MailDispatchGateway mailDispatchGateway;
    private final AdminActionLogService adminActionLogService;

    public MailingService(
            AdminMailTemplateMapper adminMailTemplateMapper,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminMailSendTargetMapper adminMailSendTargetMapper,
            AdminMailingApplicationMapper adminMailingApplicationMapper,
            AdminJobMetaMapper adminJobMetaMapper,
            AdminExportQueryMapper adminExportQueryMapper,
            ExportService exportService,
            JobService jobService,
            ClientService clientService,
            PublicFormProtectionService protectionService,
            MailDispatchGateway mailDispatchGateway,
            AdminActionLogService adminActionLogService
    ) {
        this.adminMailTemplateMapper = adminMailTemplateMapper;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminMailSendTargetMapper = adminMailSendTargetMapper;
        this.adminMailingApplicationMapper = adminMailingApplicationMapper;
        this.adminJobMetaMapper = adminJobMetaMapper;
        this.adminExportQueryMapper = adminExportQueryMapper;
        this.exportService = exportService;
        this.jobService = jobService;
        this.clientService = clientService;
        this.protectionService = protectionService;
        this.mailDispatchGateway = mailDispatchGateway;
        this.adminActionLogService = adminActionLogService;
    }

    public List<MailingHistoryItem> getHistory(Long documentSrl) {
        List<AdminMailSendJob> jobs = documentSrl == null
                ? adminMailSendJobMapper.findAll()
                : adminMailSendJobMapper.findByDocumentSrl(documentSrl);
        if (jobs.isEmpty()) {
            return List.of();
        }

        List<AdminMailSendJob> displayJobs = jobs.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(this::historyActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminMailSendJob::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<Long, String> titlesByDocumentSrl = new LinkedHashMap<>();
        List<Long> documentSrls = displayJobs.stream()
                .map(AdminMailSendJob::getDocumentSrl)
                .toList();
        for (JobListItem job : jobService.getJobsByDocumentSrls(documentSrls)) {
            titlesByDocumentSrl.put(job.getDocumentSrl(), job.getTitle());
        }
        for (AdminMailSendJob job : displayJobs) {
            job.setJobTitle(titlesByDocumentSrl.getOrDefault(job.getDocumentSrl(), "Job #" + job.getDocumentSrl()));
        }

        Map<Long, List<AdminMailSendTarget>> targetsBySendJobId = new LinkedHashMap<>();
        List<Long> sendJobIds = displayJobs.stream().map(AdminMailSendJob::getId).toList();
        for (AdminMailSendTarget target : adminMailSendTargetMapper.findBySendJobIds(sendJobIds)) {
            targetsBySendJobId.computeIfAbsent(target.getSendJobId(), ignored -> new ArrayList<>()).add(target);
        }

        Map<Long, Integer> cumulativeSentCountsBySendJobId = cumulativeSentCountsBySendJobId(displayJobs);
        List<MailingHistoryItem> historyItems = new ArrayList<>();
        for (AdminMailSendJob job : displayJobs) {
            List<AdminMailSendTarget> targets = targetsBySendJobId.getOrDefault(job.getId(), List.of());
            historyItems.add(new MailingHistoryItem(
                    job,
                    targets,
                    recipientAddressesForHistory(job, targets),
                    cumulativeSentCountsBySendJobId.getOrDefault(job.getId(), 0)
            ));
        }
        return historyItems;
    }

    private Map<Long, Integer> cumulativeSentCountsBySendJobId(List<AdminMailSendJob> jobs) {
        Map<Long, Integer> countsByDocumentSrl = new LinkedHashMap<>();
        Map<Long, Integer> countsBySendJobId = new LinkedHashMap<>();
        List<AdminMailSendJob> chronologicalJobs = jobs.stream()
                .filter(job -> job != null && job.getId() != null)
                .sorted(Comparator
                        .comparing(this::historyActivityAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(AdminMailSendJob::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (AdminMailSendJob job : chronologicalJobs) {
            Long documentSrl = job.getDocumentSrl();
            if (documentSrl == null) {
                countsBySendJobId.put(job.getId(), 0);
                continue;
            }
            if ("SENT".equals(job.getSendStatus())) {
                countsByDocumentSrl.merge(documentSrl, nullToZero(job.getTargetSnapshotCount()), Integer::sum);
            }
            countsBySendJobId.put(job.getId(), countsByDocumentSrl.getOrDefault(documentSrl, 0));
        }
        return countsBySendJobId;
    }

    private LocalDateTime historyActivityAt(AdminMailSendJob job) {
        if (job.getSentAt() != null) {
            return job.getSentAt();
        }
        if (job.getScheduledAt() != null) {
            return job.getScheduledAt();
        }
        return job.getCreatedAt();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private List<String> recipientAddressesForHistory(AdminMailSendJob job, List<AdminMailSendTarget> targets) {
        List<String> snapshotRecipients = MailingHistoryItem.recipientAddressesFromTargets(targets);
        if (snapshotRecipients.isEmpty() || snapshotRecipients.stream().noneMatch(this::looksMaskedEmail)) {
            return snapshotRecipients;
        }
        if (job == null || job.getDocumentSrl() == null) {
            return snapshotRecipients;
        }

        try {
            List<String> currentRecipients = parseRecipients(job.getDocumentSrl()).recipients();
            if (!currentRecipients.isEmpty()) {
                return currentRecipients;
            }
        } catch (RuntimeException ignored) {
            return snapshotRecipients;
        }
        return snapshotRecipients;
    }

    private boolean looksMaskedEmail(String value) {
        return value != null && value.contains("*");
    }

    public MailingPreview getPreview(Long documentSrl) {
        if (documentSrl == null) {
            return null;
        }

        Snapshot snapshot = loadSnapshot(documentSrl);
        RecipientSelection recipients = parseRecipients(documentSrl);
        String jobTitle = jobService.getJob(documentSrl).getDocument().getTitle();
        return new MailingPreview(
                documentSrl,
                jobTitle,
                recipients.recipients(),
                recipients.recipients().size(),
                recipients.excludedCount(),
                snapshot.applicationIds().size(),
                snapshot.blacklistExcludedCount()
        );
    }

    @Transactional("adminTransactionManager")
    public Long sendManual(MailSendManualForm form, AdminPrincipal principal, HttpServletRequest request) {
        Long sendJobId = sendNow(
                form.getDocumentSrl(),
                form.getTemplateId(),
                form.getDirectMailSubject(),
                form.getDirectMailBody(),
                MailAttachmentType.fromValue(form.getAttachmentType()),
                "MANUAL",
                "MANUAL",
                null,
                buildManualDuplicateKey(form.getDocumentSrl()),
                principal,
                request
        );
        return requireImmediateSendSuccess(sendJobId);
    }

    @Transactional("adminTransactionManager")
    public Long schedule(MailScheduleForm form, AdminPrincipal principal, HttpServletRequest request) {
        LocalDateTime scheduledAt = resolveScheduleAt(form);
        validateScheduledAt(scheduledAt);
        String duplicateKey = buildScheduledDuplicateKey(form);
        assertNoDuplicate(duplicateKey);

        boolean dailyRepeat = Boolean.TRUE.equals(form.getDailyRepeat());
        Snapshot snapshot = dailyRepeat ? new Snapshot(List.of(), 0) : loadSnapshot(form.getDocumentSrl());
        RecipientSelection recipients = parseRecipients(form.getDocumentSrl());
        MailContent mailContent = resolveMailContent(form.getTemplateId(), form.getDirectMailSubject(), form.getDirectMailBody());

        AdminMailSendJob sendJob = baseJob(
                form.getDocumentSrl(),
                mailContent.templateId(),
                mailContent.subject(),
                mailContent.body(),
                MailAttachmentType.fromValue(form.getAttachmentType()),
                "SCHEDULED",
                dailyRepeat ? "SCHEDULED_DAILY" : "SCHEDULED",
                recipients,
                snapshot,
                null,
                duplicateKey,
                principal.getId()
        );
        sendJob.setSendStatus(!dailyRepeat && snapshot.applicationIds().isEmpty() ? "NO_TARGETS" : "SCHEDULED");
        sendJob.setScheduledAt(scheduledAt);
        sendJob.setRepeatYn(dailyRepeat ? "Y" : "N");
        sendJob.setRepeatUnit(dailyRepeat ? "DAILY" : null);
        adminMailSendJobMapper.insert(sendJob);

        if (!dailyRepeat) {
            insertTargets(sendJob.getId(), snapshot.applicationIds(), recipients, "PENDING", null, null);
        }
        if (!dailyRepeat && !snapshot.applicationIds().isEmpty()) {
            updateApplicationDelivery(snapshot.applicationIds(), sendJob.getId(), "SCHEDULED", null);
        }

        safeLog(
                principal.getId(),
                "MAIL_SEND_SCHEDULE",
                "JOB",
                String.valueOf(form.getDocumentSrl()),
                "예약 메일 발송 작업 #" + sendJob.getId() + " 등록" + (dailyRepeat ? " (매일 반복)" : ""),
                request
        );
        return sendJob.getId();
    }

    @Transactional("adminTransactionManager")
    public void updateThresholdSettings(MailThresholdSettingsForm form, AdminPrincipal principal, HttpServletRequest request) {
        AdminJobMeta jobMeta = requireJobMeta(form.getDocumentSrl());
        boolean enabled = Boolean.TRUE.equals(form.getAutoSendEnabled());
        String autoSendEnabled = enabled ? "Y" : "N";
        String autoSendMode = enabled ? "THRESHOLD" : null;
        Integer threshold = enabled ? form.getAutoSendThreshold() : null;
        Long templateId = enabled ? form.getAutoSendTemplateId() : null;
        String attachmentType = enabled
                ? MailAttachmentType.fromValue(form.getAutoSendAttachmentType()).name()
                : DEFAULT_ATTACHMENT_TYPE;

        if (enabled && (threshold == null || threshold < 1)) {
            throw new IllegalArgumentException("임계치 자동 발송에는 유효한 임계치가 필요합니다.");
        }
        if (enabled && templateId != null) {
            requiredTemplate(templateId);
        }

        int updated = adminJobMetaMapper.updateThresholdMailSettings(
                jobMeta.getDocumentSrl(),
                autoSendEnabled,
                autoSendMode,
                threshold,
                templateId,
                attachmentType
        );
        if (updated == 0) {
            throw new IllegalStateException("공고 메일 설정을 저장하지 못했습니다.");
        }
        adminActionLogService.log(
                principal.getId(),
                "MAIL_THRESHOLD_SETTINGS_UPDATE",
                "JOB",
                String.valueOf(jobMeta.getDocumentSrl()),
                "임계치 메일 설정 저장: documentSrl=" + jobMeta.getDocumentSrl()
                        + ", autoSendEnabled=" + autoSendEnabled
                        + ", autoSendThreshold=" + threshold
                        + ", autoSendTemplateId=" + templateId
                        + ", autoSendAttachmentType=" + attachmentType,
                request
        );
    }

    @Transactional("adminTransactionManager")
    public Long triggerThreshold(MailThresholdTriggerForm form, AdminPrincipal principal, HttpServletRequest request) {
        AdminJobMeta jobMeta = requireJobMeta(form.getDocumentSrl());
        Snapshot snapshot = loadThresholdSnapshot(form.getDocumentSrl());
        int threshold = resolveManualThreshold(jobMeta, snapshot.applicationIds().size());
        if (snapshot.applicationIds().size() < threshold) {
            throw new IllegalStateException("임계치에 도달하지 않았습니다.");
        }

        String duplicateKey = buildThresholdDuplicateKey(form.getDocumentSrl(), threshold, snapshot.applicationIds().size());
        assertNoDuplicate(duplicateKey);
        Long sendJobId = sendNow(
                form.getDocumentSrl(),
                form.getTemplateId(),
                form.getDirectMailSubject(),
                form.getDirectMailBody(),
                MailAttachmentType.fromValue(form.getAttachmentType()),
                "AUTO",
                "THRESHOLD",
                threshold,
                duplicateKey,
                principal,
                request,
                snapshot
        );
        return requireImmediateSendSuccess(sendJobId);
    }

    @Transactional("adminTransactionManager")
    public void cancelSendJob(Long sendJobId, AdminPrincipal principal, HttpServletRequest request) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null) {
            throw new IllegalArgumentException("메일 발송 작업을 찾을 수 없습니다.");
        }
        if (!"SCHEDULED".equals(sendJob.getSendStatus())) {
            throw new IllegalStateException("예약 상태인 메일만 취소할 수 있습니다.");
        }

        int updated = adminMailSendJobMapper.updateStatusIfCurrent(sendJobId, "CANCELLED", null, "SCHEDULED");
        if (updated < 1) {
            throw new IllegalStateException("이미 처리 중인 메일 발송 작업입니다.");
        }

        adminMailSendTargetMapper.updateResultBySendJobId(sendJobId, "CANCELLED", "관리자가 예약 발송을 취소했습니다.", null);
        boolean dailyRepeat = isDailyRepeat(sendJob);
        Snapshot dailySnapshot = dailyRepeat ? loadDailyScheduledSnapshot(sendJob.getDocumentSrl()) : null;
        List<Long> applicationIds = dailyRepeat
                ? dailySnapshot.applicationIds()
                : adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                        .map(AdminMailSendTarget::getApplicationId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        updateApplicationDelivery(applicationIds, null, "READY", null);
        safeLog(
                principal.getId(),
                "MAIL_SEND_CANCEL",
                "MAIL_SEND_JOB",
                String.valueOf(sendJobId),
                "메일 발송 작업 #" + sendJobId + " 예약 취소",
                request
        );
    }

    public boolean executeScheduledSend(Long sendJobId) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null || !"SCHEDULED".equals(sendJob.getSendStatus())) {
            return false;
        }

        boolean dailyRepeat = isDailyRepeat(sendJob);
        List<Long> applicationIds = dailyRepeat
                ? loadDailyScheduledSnapshot(sendJob.getDocumentSrl()).applicationIds()
                : adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                        .map(AdminMailSendTarget::getApplicationId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();

        if (applicationIds.isEmpty()) {
            sendJob.setSendStatus("NO_TARGETS");
            adminMailSendJobMapper.updateStatus(sendJob);
            if (dailyRepeat) {
                scheduleNextDailySend(sendJob);
            }
            safeLog(
                    null,
                    "MAIL_SEND_SCHEDULED_EXECUTE",
                    "MAIL_SEND_JOB",
                    String.valueOf(sendJobId),
                    "예약 발송 작업 #" + sendJobId + "에 저장된 발송 대상 지원서가 없습니다.",
                    null
            );
            return false;
        }

        int claimed = adminMailSendJobMapper.updateStatusIfCurrent(sendJobId, "RUNNING", null, "SCHEDULED");
        if (claimed < 1) {
            return false;
        }
        sendJob.setSendStatus("RUNNING");
        sendJob.setSentAt(null);

        String sendStatus;
        String targetResult;
        String failReason = null;
        LocalDateTime sentAt = null;
        RecipientSelection recipients = null;

        try {
            recipients = parseRecipients(sendJob.getDocumentSrl());
            if (recipients.recipients().isEmpty()) {
                throw new IllegalStateException("등록된 거래처 수신 이메일이 없습니다.");
            }

            MailContent mailContent = resolveStoredMailContent(sendJob);
            MailAttachmentType attachmentType = MailAttachmentType.fromValue(
                    sendJob.getAttachmentType() == null ? DEFAULT_ATTACHMENT_TYPE : sendJob.getAttachmentType()
            );
            ExportPayload attachment = attachmentType == MailAttachmentType.XLSX
                    ? exportService.prepareXlsx(sendJob.getDocumentSrl(), applicationIds)
                    : exportService.prepareTxt(sendJob.getDocumentSrl(), applicationIds);
            mailDispatchGateway.dispatch(buildDispatchRequest(
                    sendJob.getDocumentSrl(),
                    recipients.recipients(),
                    mailContent,
                    attachment,
                    attachmentType,
                    dailyRepeat ? "SCHEDULED_DAILY" : "SCHEDULED",
                    applicationIds.size()
            ));
            sendStatus = "SENT";
            targetResult = "SENT";
            sentAt = LocalDateTime.now();
        } catch (Exception ex) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = trimFailureReason(ex.getMessage());
        }

        sendJob.setSendStatus(sendStatus);
        sendJob.setSentAt(sentAt);
        adminMailSendJobMapper.updateStatus(sendJob);
        if (dailyRepeat) {
            insertTargets(sendJobId, applicationIds, recipients == null ? new RecipientSelection(List.of(), 0, "Client") : recipients, targetResult, failReason, sentAt);
            scheduleNextDailySend(sendJob);
        } else {
            adminMailSendTargetMapper.updateResultBySendJobId(sendJobId, targetResult, failReason, sentAt);
        }
        updateApplicationDelivery(applicationIds, sendJobId, "SENT".equals(sendStatus) ? "SENT" : "FAILED", sentAt);
        safeLog(
                null,
                "MAIL_SEND_SCHEDULED_EXECUTE",
                "MAIL_SEND_JOB",
                String.valueOf(sendJobId),
                "예약 발송 작업 #" + sendJobId + " 처리 완료: " + displaySendStatus(sendStatus),
                null
        );
        return "SENT".equals(sendStatus);
    }

    @Transactional("adminTransactionManager")
    public boolean triggerThresholdAutomatically(Long documentSrl) {
        AdminJobMeta jobMeta = requireJobMeta(documentSrl);
        if (!"Y".equals(jobMeta.getAutoSendEnabled()) || !"THRESHOLD".equals(jobMeta.getAutoSendMode())) {
            return false;
        }
        if (jobMeta.getAutoSendThreshold() == null || jobMeta.getAutoSendThreshold() < 1) {
            return false;
        }
        Snapshot snapshot = loadThresholdSnapshot(documentSrl);
        if (snapshot.applicationIds().size() < jobMeta.getAutoSendThreshold()) {
            return false;
        }

        String duplicateKey = buildThresholdDuplicateKey(documentSrl, jobMeta.getAutoSendThreshold(), snapshot.applicationIds().size());
        AdminMailSendJob existing = adminMailSendJobMapper.findByDuplicatePreventKey(duplicateKey);
        if (existing != null && !"FAILED".equals(existing.getSendStatus())) {
            return false;
        }

        sendNow(
                documentSrl,
                jobMeta.getAutoSendTemplateId(),
                jobMeta.getAutoSendTemplateId() == null ? DEFAULT_DIRECT_SUBJECT : null,
                jobMeta.getAutoSendTemplateId() == null ? DEFAULT_DIRECT_BODY : null,
                MailAttachmentType.fromValue(
                        jobMeta.getAutoSendAttachmentType() == null ? DEFAULT_ATTACHMENT_TYPE : jobMeta.getAutoSendAttachmentType()
                ),
                "AUTO",
                "THRESHOLD",
                jobMeta.getAutoSendThreshold(),
                duplicateKey,
                new AdminPrincipal(null, "scheduler", "", "Scheduler", "Y", null),
                null,
                snapshot
        );
        adminJobMetaMapper.updateSchedulerState(documentSrl, LocalDateTime.now(), jobMeta.getNextAutoSendAt());
        return true;
    }

    private Long sendNow(
            Long documentSrl,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            String sendType,
            String triggerType,
            Integer thresholdSnapshot,
            String duplicatePreventKey,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return sendNow(
                documentSrl,
                templateId,
                directMailSubject,
                directMailBody,
                attachmentType,
                sendType,
                triggerType,
                thresholdSnapshot,
                duplicatePreventKey,
                principal,
                request,
                null
        );
    }

    private Long sendNow(
            Long documentSrl,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            String sendType,
            String triggerType,
            Integer thresholdSnapshot,
            String duplicatePreventKey,
            AdminPrincipal principal,
            HttpServletRequest request,
            Snapshot snapshotOverride
    ) {
        Snapshot snapshot = snapshotOverride != null ? snapshotOverride : loadSnapshot(documentSrl);
        RecipientSelection recipients = parseRecipients(documentSrl);
        MailContent mailContent = resolveMailContent(templateId, directMailSubject, directMailBody);

        AdminMailSendJob sendJob = baseJob(
                documentSrl,
                mailContent.templateId(),
                mailContent.subject(),
                mailContent.body(),
                attachmentType,
                sendType,
                triggerType,
                recipients,
                snapshot,
                thresholdSnapshot,
                duplicatePreventKey,
                principal.getId()
        );
        sendJob.setSendStatus("PENDING");
        adminMailSendJobMapper.insert(sendJob);

        if (snapshot.applicationIds().isEmpty()) {
            sendJob.setSendStatus("NO_TARGETS");
            adminMailSendJobMapper.updateStatus(sendJob);
            safeLog(
                    principal.getId(),
                    "MAIL_SEND_" + triggerType,
                    "JOB",
                    String.valueOf(documentSrl),
                    "메일 발송 작업 #" + sendJob.getId() + " 생성: 발송 대상 지원서 없음",
                    request
            );
            return sendJob.getId();
        }

        String sendStatus;
        String targetResult;
        String failReason = null;
        LocalDateTime sentAt = null;

        if (recipients.recipients().isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = "등록된 거래처 수신 이메일이 없습니다.";
        } else {
            ExportPayload attachment = attachmentType == MailAttachmentType.XLSX
                    ? exportService.prepareXlsx(documentSrl, snapshot.applicationIds())
                    : exportService.prepareTxt(documentSrl, snapshot.applicationIds());
            try {
                mailDispatchGateway.dispatch(buildDispatchRequest(documentSrl, recipients.recipients(), mailContent, attachment, attachmentType, triggerType, snapshot.applicationIds().size()));
                sendStatus = "SENT";
                targetResult = "SENT";
                sentAt = LocalDateTime.now();
            } catch (Exception ex) {
                sendStatus = "FAILED";
                targetResult = "FAILED";
                failReason = trimFailureReason(ex.getMessage());
            }
        }

        sendJob.setSendStatus(sendStatus);
        sendJob.setSentAt(sentAt);
        adminMailSendJobMapper.updateStatus(sendJob);
        insertTargets(sendJob.getId(), snapshot.applicationIds(), recipients, targetResult, failReason, sentAt);
        updateApplicationDelivery(snapshot.applicationIds(), sendJob.getId(), "SENT".equals(sendStatus) ? "SENT" : "FAILED", sentAt);

        safeLog(
                principal.getId(),
                "MAIL_SEND_" + triggerType,
                "JOB",
                String.valueOf(documentSrl),
                "메일 발송 작업 #" + sendJob.getId() + " 처리 완료: " + displaySendStatus(sendStatus),
                request
        );
        return sendJob.getId();
    }

    private Long requireImmediateSendSuccess(Long sendJobId) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null) {
            throw new IllegalStateException("메일 발송 결과를 확인할 수 없습니다.");
        }
        if ("SENT".equals(sendJob.getSendStatus())) {
            return sendJobId;
        }
        if ("NO_TARGETS".equals(sendJob.getSendStatus())) {
            throw new IllegalStateException("발송 대상 지원서가 없어 메일을 보내지 않았습니다.");
        }
        if ("FAILED".equals(sendJob.getSendStatus())) {
            String failReason = adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                    .map(AdminMailSendTarget::getFailReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .findFirst()
                    .orElse("메일 발송에 실패했습니다.");
            throw new IllegalStateException(failReason);
        }
        throw new IllegalStateException("메일 발송 상태를 확인해 주세요: " + sendJob.getSendStatus());
    }

    private AdminMailSendJob baseJob(
            Long documentSrl,
            Long templateId,
            String mailSubjectSnapshot,
            String mailBodySnapshot,
            MailAttachmentType attachmentType,
            String sendType,
            String triggerType,
            RecipientSelection recipients,
            Snapshot snapshot,
            Integer thresholdSnapshot,
            String duplicatePreventKey,
            Long createdBy
    ) {
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setDocumentSrl(documentSrl);
        sendJob.setTemplateId(templateId);
        sendJob.setMailSubjectSnapshot(mailSubjectSnapshot);
        sendJob.setMailBodySnapshot(mailBodySnapshot);
        sendJob.setAttachmentType(attachmentType.name());
        sendJob.setSendType(sendType);
        sendJob.setTriggerType(triggerType);
        sendJob.setRecipientCount(recipients.recipients().size());
        sendJob.setExcludedCount(recipients.excludedCount());
        sendJob.setBlacklistExcludedCount(snapshot.blacklistExcludedCount());
        sendJob.setThresholdSnapshot(thresholdSnapshot);
        sendJob.setTargetSnapshotCount(snapshot.applicationIds().size());
        sendJob.setDuplicatePreventKey(duplicatePreventKey);
        sendJob.setRepeatYn("N");
        sendJob.setCreatedBy(createdBy);
        return sendJob;
    }

    private Snapshot loadSnapshot(Long documentSrl) {
        return loadSnapshot(documentSrl, null);
    }

    private Snapshot loadThresholdSnapshot(Long documentSrl) {
        return loadSnapshot(documentSrl, adminMailSendJobMapper.findLastSuccessfulThresholdSentAt(documentSrl));
    }

    private Snapshot loadDailyScheduledSnapshot(Long documentSrl) {
        return loadSnapshot(documentSrl, adminMailSendJobMapper.findLastSuccessfulDailyScheduledSentAt(documentSrl));
    }

    private Snapshot loadSnapshot(Long documentSrl, LocalDateTime appliedAfter) {
        List<Long> includedApplicationIds = new ArrayList<>();
        int blacklistExcludedCount = 0;
        for (ExportApplicationSource application : adminExportQueryMapper.findApplicationsByDocumentSrl(documentSrl)) {
            if (appliedAfter != null && (application.getAppliedAt() == null || !application.getAppliedAt().isAfter(appliedAfter))) {
                continue;
            }
            if (!"Y".equals(application.getProvideYn())) {
                continue;
            }
            if ("Y".equals(application.getIsBlacklisted())) {
                blacklistExcludedCount++;
                continue;
            }
            includedApplicationIds.add(application.getId());
        }
        return new Snapshot(includedApplicationIds, blacklistExcludedCount);
    }

    private RecipientSelection parseRecipients(Long documentSrl) {
        AdminJobMeta jobMeta = requireJobMeta(documentSrl);
        List<String> rawValues = new ArrayList<>();
        if (jobMeta.getClientId() != null) {
            for (String email : clientService.getActiveRecipientEmails(jobMeta.getClientId())) {
                addRawRecipients(rawValues, email);
            }
        } else {
            addRawRecipients(rawValues, jobMeta.getClientEmail());
            addRawRecipients(rawValues, jobMeta.getClientEmails());
        }

        Set<String> deduplicatedKeys = new LinkedHashSet<>();
        List<String> recipients = new ArrayList<>();
        int excludedCount = 0;
        for (String value : rawValues) {
            String normalized = value.trim();
            if (!isValidEmail(normalized)) {
                excludedCount++;
                continue;
            }
            String key = normalized.toLowerCase(Locale.ROOT);
            if (!deduplicatedKeys.add(key)) {
                excludedCount++;
                continue;
            }
            recipients.add(normalized);
        }
        String targetName = jobMeta.getClientId() != null
                ? blankToDefault(clientService.getClientSummary(jobMeta.getClientId()).clientName(), "Client")
                : blankToDefault(jobMeta.getClientName(), "Client");
        return new RecipientSelection(recipients, excludedCount, targetName);
    }

    private AdminMailTemplate requiredTemplate(Long templateId) {
        AdminMailTemplate template = adminMailTemplateMapper.findById(templateId);
        if (template == null || !template.isActive()) {
            throw new IllegalArgumentException("사용 가능한 메일 템플릿을 찾을 수 없습니다.");
        }
        return template;
    }

    private MailContent resolveMailContent(Long templateId, String directMailSubject, String directMailBody) {
        if (templateId != null) {
            AdminMailTemplate template = requiredTemplate(templateId);
            return new MailContent(template.getId(), template.getMailSubject(), template.getMailBody());
        }
        String subject = trimToNull(directMailSubject);
        String body = trimToNull(directMailBody);
        if (subject == null || body == null) {
            throw new IllegalArgumentException("템플릿을 선택하거나 직접 작성 제목과 본문을 입력해 주세요.");
        }
        if (subject.length() > 255) {
            throw new IllegalArgumentException("메일 제목은 255자 이하로 입력해 주세요.");
        }
        return new MailContent(null, subject, body);
    }

    private MailContent resolveStoredMailContent(AdminMailSendJob sendJob) {
        String subject = trimToNull(sendJob.getMailSubjectSnapshot());
        String body = trimToNull(sendJob.getMailBodySnapshot());
        if (subject != null && body != null) {
            return new MailContent(sendJob.getTemplateId(), subject, body);
        }
        AdminMailTemplate template = requiredTemplate(sendJob.getTemplateId());
        return new MailContent(template.getId(), template.getMailSubject(), template.getMailBody());
    }

    private AdminJobMeta requireJobMeta(Long documentSrl) {
        AdminJobMeta jobMeta = jobService.ensureJobMeta(documentSrl);
        if (jobMeta == null) {
            throw new IllegalArgumentException("공고 메타 정보를 찾을 수 없습니다.");
        }
        return jobMeta;
    }

    private MailDispatchRequest buildDispatchRequest(
            Long documentSrl,
            List<String> recipients,
            MailContent mailContent,
            ExportPayload attachment,
            MailAttachmentType attachmentType,
            String triggerType,
            int applicationCount
    ) {
        AdminJobMeta jobMeta = requireJobMeta(documentSrl);
        String replyTo = jobMeta.getClientId() != null
                ? clientService.getClientSummary(jobMeta.getClientId()).replyToEmail()
                : null;
        Map<String, String> variables = buildTemplateVariables(documentSrl, attachmentType, triggerType, applicationCount);
        return new MailDispatchRequest(
                recipients,
                replyTo,
                renderTemplate(mailContent.subject(), variables),
                renderTemplate(mailContent.body(), variables),
                attachment.fileName(),
                attachment.contentType(),
                attachment.content()
        );
    }

    private Map<String, String> buildTemplateVariables(
            Long documentSrl,
            MailAttachmentType attachmentType,
            String triggerType,
            int applicationCount
    ) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("jobTitle", jobService.getJob(documentSrl).getDocument().getTitle());
        variables.put("documentSrl", String.valueOf(documentSrl));
        variables.put("applicationCount", String.valueOf(applicationCount));
        variables.put("attachmentType", attachmentType.name());
        variables.put("triggerType", triggerType);
        variables.put("sentAt", LocalDateTime.now().format(MAIL_DT));
        return variables;
    }

    private void assertNoDuplicate(String duplicatePreventKey) {
        AdminMailSendJob existing = adminMailSendJobMapper.findByDuplicatePreventKey(duplicatePreventKey);
        if (existing != null && !"FAILED".equals(existing.getSendStatus())) {
            throw new IllegalStateException("동일한 메일 발송 작업이 이미 존재합니다.");
        }
    }

    private void insertTargets(
            Long sendJobId,
            List<Long> applicationIds,
            RecipientSelection recipients,
            String sendResult,
            String failReason,
            LocalDateTime sentAt
    ) {
        if (applicationIds.isEmpty() || recipients.recipients().isEmpty()) {
            return;
        }
        for (Long applicationId : applicationIds) {
            for (String recipient : recipients.recipients()) {
                AdminMailSendTarget target = new AdminMailSendTarget();
                target.setSendJobId(sendJobId);
                target.setApplicationId(applicationId);
                target.setTargetEmailMasked(recipient);
                target.setTargetName(recipients.targetName());
                target.setSendResult(sendResult);
                target.setFailReason(failReason);
                target.setSentAt(sentAt);
                adminMailSendTargetMapper.insert(target);
            }
        }
    }

    private void updateApplicationDelivery(
            List<Long> applicationIds,
            Long deliveryJobId,
            String deliveryStatus,
            LocalDateTime deliveredAt
    ) {
        for (Long applicationId : applicationIds) {
            adminMailingApplicationMapper.updateDeliveryStatus(applicationId, deliveryStatus, deliveryJobId, deliveredAt);
        }
    }

    private String renderTemplate(String templateText, Map<String, String> variables) {
        String rendered = templateText;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isValidEmail(String value) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void addRawRecipients(List<String> target, String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        for (String token : source.split("[,;\\s]+")) {
            if (!token.isBlank()) {
                target.add(token);
            }
        }
    }

    private String buildManualDuplicateKey(Long documentSrl) {
        return "MANUAL:" + documentSrl + ":" + System.currentTimeMillis();
    }

    private String buildScheduledDuplicateKey(MailScheduleForm form) {
        LocalDateTime scheduledAt = resolveScheduleAt(form);
        String prefix = Boolean.TRUE.equals(form.getDailyRepeat()) ? "SCHEDULED_DAILY" : "SCHEDULED";
        return prefix + ":" + form.getDocumentSrl() + ":" + scheduledAt.withNano(0);
    }

    private String buildThresholdDuplicateKey(Long documentSrl, int threshold, int applicationCount) {
        return "THRESHOLD:" + documentSrl + ":" + threshold + ":" + applicationCount;
    }

    private int resolveManualThreshold(AdminJobMeta jobMeta, int applicationCount) {
        if (jobMeta.getAutoSendThreshold() != null && jobMeta.getAutoSendThreshold() > 0) {
            return jobMeta.getAutoSendThreshold();
        }
        return applicationCount;
    }

    private void validateScheduledAt(LocalDateTime scheduledAt) {
        LocalDateTime minimumScheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        if (scheduledAt == null || scheduledAt.isBefore(minimumScheduledAt)) {
            throw new IllegalArgumentException("예약 발송 시각은 현재 시각보다 최소 1분 이후여야 합니다.");
        }
    }

    private LocalDateTime resolveScheduleAt(MailScheduleForm form) {
        if (!Boolean.TRUE.equals(form.getDailyRepeat())) {
            return form.getScheduledAt();
        }
        LocalTime sendTime = form.getDailySendTime();
        if (sendTime == null) {
            sendTime = form.getScheduledAt() == null ? null : form.getScheduledAt().toLocalTime();
        }
        if (sendTime == null) {
            throw new IllegalArgumentException("매일 반복 발송 시각을 입력해 주세요.");
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime scheduledAt = now.toLocalDate().atTime(sendTime).truncatedTo(ChronoUnit.MINUTES);
        return scheduledAt.isAfter(now) ? scheduledAt : scheduledAt.plusDays(1);
    }

    private boolean isDailyRepeat(AdminMailSendJob sendJob) {
        return "Y".equals(sendJob.getRepeatYn()) && "DAILY".equals(sendJob.getRepeatUnit());
    }

    private void scheduleNextDailySend(AdminMailSendJob completedJob) {
        LocalDateTime base = completedJob.getScheduledAt() == null ? LocalDateTime.now() : completedJob.getScheduledAt();
        LocalDateTime nextScheduledAt = base.plusDays(1);
        String duplicateKey = "SCHEDULED_DAILY:" + completedJob.getDocumentSrl() + ":" + nextScheduledAt.withNano(0);
        if (adminMailSendJobMapper.findByDuplicatePreventKey(duplicateKey) != null) {
            return;
        }
        RecipientSelection recipients = parseRecipients(completedJob.getDocumentSrl());
        AdminMailSendJob nextJob = baseJob(
                completedJob.getDocumentSrl(),
                completedJob.getTemplateId(),
                completedJob.getMailSubjectSnapshot(),
                completedJob.getMailBodySnapshot(),
                MailAttachmentType.fromValue(completedJob.getAttachmentType() == null ? DEFAULT_ATTACHMENT_TYPE : completedJob.getAttachmentType()),
                "SCHEDULED",
                "SCHEDULED_DAILY",
                recipients,
                new Snapshot(List.of(), 0),
                null,
                duplicateKey,
                completedJob.getCreatedBy()
        );
        nextJob.setSendStatus("SCHEDULED");
        nextJob.setScheduledAt(nextScheduledAt);
        nextJob.setRepeatYn("Y");
        nextJob.setRepeatUnit("DAILY");
        adminMailSendJobMapper.insert(nextJob);
    }

    private void safeLog(
            Long adminUserId,
            String actionType,
            String targetType,
            String targetId,
            String actionDetail,
            HttpServletRequest request
    ) {
        try {
            adminActionLogService.log(adminUserId, actionType, targetType, targetId, actionDetail, request);
        } catch (RuntimeException ignored) {
            // 외부 메일 발송이 성공한 뒤에는 로그 실패로 발송 상태가 롤백되면 안 됩니다.
        }
    }

    private String trimFailureReason(String message) {
        if (message == null || message.isBlank()) {
            return "메일 발송에 실패했습니다.";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String displaySendStatus(String sendStatus) {
        return switch (sendStatus) {
            case "SENT" -> "발송완료";
            case "FAILED" -> "실패";
            case "NO_TARGETS" -> "발송 대상 없음";
            case "SCHEDULED" -> "예약중";
            case "RUNNING" -> "실행중";
            case "CANCELLED" -> "취소";
            default -> sendStatus;
        };
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private record Snapshot(List<Long> applicationIds, int blacklistExcludedCount) {
    }

    private record RecipientSelection(List<String> recipients, int excludedCount, String targetName) {
    }

    private record MailContent(Long templateId, String subject, String body) {
    }
}
