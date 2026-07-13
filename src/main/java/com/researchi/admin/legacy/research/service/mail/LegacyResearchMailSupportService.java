package com.researchi.admin.legacy.research.service.mail;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.ResearchApplicationService;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientSelection;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import com.researchi.admin.mailing.mapper.AdminMailApplicationClaimMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class LegacyResearchMailSupportService {

    private static final String DEFAULT_ATTACHMENT_TYPE = "XLSX";
    public static final String CLAIMED_BY_OTHER_SEND_REASON = "Another mail send job is already processing this applicant.";
    private static final int CLAIM_EXPIRY_MINUTES = 120;
    private static final int MAX_MAIL_SUBJECT_LENGTH = 255;
    private static final String INTRODUCER_MAIL_LABEL = " - 소개자 하진혁(010-2875-3457)";
    public static final String NO_UNPROVIDED_DATA_REASON = "PROVIDE_YN=N인 발송 대상 신청자 정보가 없습니다.";
    private static final DateTimeFormatter MAIL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminMailTemplateMapper adminMailTemplateMapper;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminMailSendTargetMapper adminMailSendTargetMapper;
    private final AdminMailApplicationClaimMapper adminMailApplicationClaimMapper;
    private final ResearchApplicationMapper researchApplicationMapper;
    private final ResearchApplicationService researchApplicationService;
    private final AdminActionLogService adminActionLogService;

    public LegacyResearchMailSupportService(
            AdminMailTemplateMapper adminMailTemplateMapper,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminMailSendTargetMapper adminMailSendTargetMapper,
            AdminMailApplicationClaimMapper adminMailApplicationClaimMapper,
            ResearchApplicationMapper researchApplicationMapper,
            ResearchApplicationService researchApplicationService,
            AdminActionLogService adminActionLogService
    ) {
        this.adminMailTemplateMapper = adminMailTemplateMapper;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminMailSendTargetMapper = adminMailSendTargetMapper;
        this.adminMailApplicationClaimMapper = adminMailApplicationClaimMapper;
        this.researchApplicationMapper = researchApplicationMapper;
        this.researchApplicationService = researchApplicationService;
        this.adminActionLogService = adminActionLogService;
    }

    public String defaultAttachmentType() {
        return DEFAULT_ATTACHMENT_TYPE;
    }

    public LegacyResearchMailContent resolveMailContent(Long templateId, String directMailSubject, String directMailBody) {
        if (templateId != null) {
            AdminMailTemplate template = adminMailTemplateMapper.findById(templateId);
            if (template == null || !template.isActive()) {
                throw new IllegalArgumentException("사용할 수 없는 메일 템플릿입니다.");
            }
            return new LegacyResearchMailContent(template.getId(), template.getMailSubject(), template.getMailBody());
        }
        String subject = trimToNull(directMailSubject);
        if (subject == null) {
            throw new IllegalArgumentException("메일 제목을 입력해 주세요.");
        }
        if (subject.length() > 255) {
            throw new IllegalArgumentException("메일 제목은 255자 이내로 입력해 주세요.");
        }
        return new LegacyResearchMailContent(null, subject, trimToNull(directMailBody) == null ? "" : directMailBody);
    }

    public LegacyResearchMailContent resolveStoredMailContent(AdminMailSendJob sendJob) {
        String subject = trimToNull(sendJob.getMailSubjectSnapshot());
        if (sendJob.getTemplateId() == null && subject != null) {
            return new LegacyResearchMailContent(null, subject, sendJob.getMailBodySnapshot() == null ? "" : sendJob.getMailBodySnapshot());
        }
        String body = trimToNull(sendJob.getMailBodySnapshot());
        if (subject != null && body != null) {
            return new LegacyResearchMailContent(sendJob.getTemplateId(), subject, body);
        }
        AdminMailTemplate template = adminMailTemplateMapper.findById(sendJob.getTemplateId());
        if (template == null || !template.isActive()) {
            throw new IllegalArgumentException("사용할 수 없는 메일 템플릿입니다.");
        }
        return new LegacyResearchMailContent(template.getId(), template.getMailSubject(), template.getMailBody());
    }
    public AdminMailSendJob baseJob(
            Long researchNo,
            Long templateId,
            String mailSubjectSnapshot,
            String mailBodySnapshot,
            MailAttachmentType attachmentType,
            String sendType,
            String triggerType,
            LegacyResearchRecipientSelection recipients,
            LegacyResearchMailSnapshot snapshot,
            Integer thresholdSnapshot,
            String duplicatePreventKey,
            Long createdBy
    ) {
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setResearchNo(researchNo);
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

    public MailDispatchRequest buildDispatchRequest(
            ResearchMaster researchMaster,
            List<String> recipients,
            LegacyResearchMailContent mailContent,
            ExportPayload attachment,
            MailAttachmentType attachmentType,
            int applicationCount,
            List<Long> applicationIds
    ) {
        Map<String, String> variables = buildTemplateVariables(researchMaster, attachmentType, applicationCount);
        return new MailDispatchRequest(
                recipients,
                null,
                defaultSubject(researchMaster),
                buildProvidedDataBody(researchMaster.getResearchNo(), applicationIds),
                attachment.fileName(),
                attachment.contentType(),
                attachment.content()
        );
    }

    public String defaultSubject(ResearchMaster researchMaster) {
        String subject = trimToNull(researchMaster.getResearchTitle()) + INTRODUCER_MAIL_LABEL;
        return subject.length() <= MAX_MAIL_SUBJECT_LENGTH ? subject : subject.substring(0, MAX_MAIL_SUBJECT_LENGTH);
    }

    public void assertNoDuplicate(String duplicatePreventKey) {
        AdminMailSendJob existing = adminMailSendJobMapper.findByDuplicatePreventKey(duplicatePreventKey);
        if (isActiveDuplicate(existing)) {
            throw new IllegalStateException("같은 조건의 메일 발송 작업이 이미 등록되어 있습니다.");
        }
    }

    public boolean isActiveDuplicate(AdminMailSendJob sendJob) {
        return sendJob != null
                && !"FAILED".equals(sendJob.getSendStatus())
                && !"CANCELLED".equals(sendJob.getSendStatus())
                && !"NO_TARGETS".equals(sendJob.getSendStatus());
    }

    public void validateScheduledAt(LocalDateTime scheduledAt) {
        LocalDateTime minimumScheduledAt = minimumScheduledAt();
        if (scheduledAt == null || scheduledAt.isBefore(minimumScheduledAt)) {
            throw new IllegalArgumentException("예약 발송은 현재 시각보다 최소 2분 뒤부터 등록할 수 있습니다.");
        }
    }

    public LocalDateTime minimumScheduledAt() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minimum = now.plusMinutes(2).truncatedTo(ChronoUnit.MINUTES);
        return minimum.isBefore(now.plusMinutes(2)) ? minimum.plusMinutes(1) : minimum;
    }

    public LocalDateTime resolveDailyScheduledAt(LocalTime dailyScheduledTime) {
        if (dailyScheduledTime == null) {
            throw new IllegalArgumentException("매일 발송 시간을 입력해 주세요.");
        }
        LocalDateTime candidate = LocalDateTime.now()
                .withHour(dailyScheduledTime.getHour())
                .withMinute(dailyScheduledTime.getMinute())
                .withSecond(0)
                .withNano(0);
        return candidate.isBefore(minimumScheduledAt()) ? candidate.plusDays(1) : candidate;
    }

    public void insertTargets(
            Long sendJobId,
            List<Long> applicationIds,
            LegacyResearchRecipientSelection recipients,
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

    public void markScheduledBlacklistExcludedTargets(Long sendJobId, List<Long> applicationIds, LocalDateTime sentAt) {
        updateScheduledTargets(sendJobId, applicationIds, "EXCLUDED", "Blacklisted before scheduled send execution.", sentAt);
    }

    public List<Long> claimApplications(Long researchNo, List<Long> applicationIds, Long sendJobId) {
        if (researchNo == null || applicationIds == null || applicationIds.isEmpty() || sendJobId == null) {
            return List.of();
        }
        adminMailApplicationClaimMapper.deleteExpired(LocalDateTime.now().minusMinutes(CLAIM_EXPIRY_MINUTES));
        List<Long> claimedIds = new java.util.ArrayList<>();
        for (Long applicationId : applicationIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (adminMailApplicationClaimMapper.insertIgnore(researchNo, applicationId, sendJobId) > 0) {
                claimedIds.add(applicationId);
            }
        }
        return claimedIds;
    }

    public void releaseApplicationClaims(Long sendJobId) {
        if (sendJobId != null) {
            adminMailApplicationClaimMapper.deleteBySendJobId(sendJobId);
        }
    }

    public void updateScheduledTargets(
            Long sendJobId,
            List<Long> applicationIds,
            String sendResult,
            String failReason,
            LocalDateTime sentAt
    ) {
        if (applicationIds.isEmpty()) {
            return;
        }
        adminMailSendTargetMapper.updateResultBySendJobIdAndApplicationIds(sendJobId, applicationIds, sendResult, failReason, sentAt);
    }

    public void markApplicationsProvided(Long researchNo, List<Long> researchAppSeqs, Long changedBy, String source) {
        if (researchNo == null || researchAppSeqs == null || researchAppSeqs.isEmpty()) {
            return;
        }
        List<Long> requestedSeqs = researchAppSeqs.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedSeqs.isEmpty()) {
            return;
        }
        Set<Long> unprovidedSeqs = researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(researchNo, requestedSeqs).stream()
                .map(ResearchApplication::getResearchAppSeq)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (Long researchAppSeq : unprovidedSeqs) {
            try {
                researchApplicationService.updateProvideYn(researchNo, researchAppSeq, "Y", changedBy);
            } catch (RuntimeException ex) {
                safeLog(
                        changedBy,
                        "LEGACY_PROVIDE_UPDATE_FAILED",
                        "RESEARCH_APP",
                        researchNo + ":" + researchAppSeq,
                        "Failed to mark PROVIDE_YN=Y after " + source + ": " + trimFailureReason(ex.getMessage()),
                        null
                );
            }
        }
    }

    public void recordProviderResult(AdminMailSendJob sendJob, MailDispatchResult result) {
        if (sendJob == null || sendJob.getId() == null || result == null) {
            return;
        }
        sendJob.setMailProvider(result.provider());
        sendJob.setProviderRequestId(result.providerRequestId());
        sendJob.setProviderStatusCode(result.providerStatusCode());
        sendJob.setProviderStatusLabel(result.providerStatusLabel());
        sendJob.setProviderRawResponse(trimProviderRawResponse(result.providerRawResponse()));
        sendJob.setProviderRequestedAt(result.providerRequestedAt());
        sendJob.setProviderCheckedAt(null);
        adminMailSendJobMapper.updateProviderResult(sendJob);
    }

    public Long requireImmediateSendSuccess(Long sendJobId) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null) {
            throw new IllegalStateException("메일 발송 작업을 찾을 수 없습니다.");
        }
        if ("SENT".equals(sendJob.getSendStatus())) {
            return sendJobId;
        }
        if ("NO_TARGETS".equals(sendJob.getSendStatus())) {
            throw new IllegalStateException("발송 대상 신청자 정보가 없습니다.");
        }
        if ("FAILED".equals(sendJob.getSendStatus())) {
            String failReason = adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                    .map(AdminMailSendTarget::getFailReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .findFirst()
                    .orElse(NO_UNPROVIDED_DATA_REASON);
            throw new IllegalStateException(failReason);
        }
        throw new IllegalStateException("메일 발송 작업이 완료되지 않았습니다. " + sendJob.getSendStatus());
    }

    public void safeLog(
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
            // 메일 발송 실패 로그가 본 작업을 중단시키지 않도록 무시합니다.
        }
    }

    public String trimFailureReason(String message) {
        if (message == null || message.isBlank()) {
            return "메일 발송 중 오류가 발생했습니다.";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public String displaySendStatus(String sendStatus) {
        return switch (sendStatus) {
            case "SENT" -> "발송 완료";
            case "FAILED" -> "실패";
            case "NO_TARGETS" -> "발송 대상 없음";
            case "SCHEDULED" -> "예약중";
            case "RUNNING" -> "실행중";
            case "CANCELLED" -> "취소";
            default -> sendStatus;
        };
    }

    public String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimProviderRawResponse(String value) {
        if (value == null || value.length() <= 4000) {
            return value;
        }
        return value.substring(0, 4000);
    }

    private Map<String, String> buildTemplateVariables(
            ResearchMaster researchMaster,
            MailAttachmentType attachmentType,
            int applicationCount
    ) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("jobTitle", researchMaster.getResearchTitle());
        variables.put("researchNo", String.valueOf(researchMaster.getResearchNo()));
        variables.put("applicationCount", String.valueOf(applicationCount));
        variables.put("attachmentType", attachmentType.name());
        variables.put("triggerType", "LEGACY_MANUAL");
        variables.put("sentAt", LocalDateTime.now().format(MAIL_DT));
        return variables;
    }

    private String renderTemplate(String templateText, Map<String, String> variables) {
        String rendered = templateText;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String buildProvidedDataBody(Long researchNo, List<Long> applicationIds) {
        List<ResearchApplication> applications = researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(researchNo, applicationIds);
        StringBuilder body = new StringBuilder("성명/성별/생년월일/나이(만)/직업/회사 학교/휴대폰/유선전화/주소/추가기재사항")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        for (int index = 0; index < applications.size(); index++) {
            if (index > 0) {
                body.append(System.lineSeparator());
            }
            body.append(providedDataLine(applications.get(index)));
        }
        return body.toString();
    }

    private String providedDataLine(ResearchApplication application) {
        return String.join("/",
                display(application.getAppName()),
                display(application.getAppSexLabel()),
                display(application.getAppBirth()),
                display(application.getAppAge()),
                display(application.getAppJob()),
                display(application.getAppCompany()),
                display(application.getAppHphoneLabel()),
                display(application.getAppTeleLabel()),
                display(application.getAppAddr()),
                displayAdditionalComment(application.getAddComment())
        );
    }

    private String display(String value) {
        return value == null ? "" : value.trim();
    }

    private String displayAdditionalComment(String value) {
        return display(value)
                .replace("\r\n", " / ")
                .replace('\n', '/')
                .replace('\r', '/')
                .replaceAll("\\s*/\\s*", " / ")
                .replaceAll("(\\s*/\\s*)+", " / ")
                .trim();
    }
}
