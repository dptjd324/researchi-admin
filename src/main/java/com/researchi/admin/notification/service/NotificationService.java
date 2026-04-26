package com.researchi.admin.notification.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.matching.domain.AdminKeywordMatchTarget;
import com.researchi.admin.matching.mapper.AdminKeywordMatchTargetMapper;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.domain.NotificationApplicationRecipient;
import com.researchi.admin.notification.domain.NotificationEmailRequest;
import com.researchi.admin.notification.domain.NotificationSmsRequest;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.notification.mapper.NotificationApplicationMapper;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper;
    private final NotificationApplicationMapper notificationApplicationMapper;
    private final AdminNotificationLogMapper adminNotificationLogMapper;
    private final ApplicantNotificationMailGateway applicantNotificationMailGateway;
    private final ApplicantNotificationSmsGateway applicantNotificationSmsGateway;
    private final JobService jobService;
    private final AdminMailTemplateMapper adminMailTemplateMapper;
    private final NotificationProperties notificationProperties;
    private final PublicFormProtectionService protectionService;
    private final AdminActionLogService adminActionLogService;

    public NotificationService(
            AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper,
            NotificationApplicationMapper notificationApplicationMapper,
            AdminNotificationLogMapper adminNotificationLogMapper,
            ApplicantNotificationMailGateway applicantNotificationMailGateway,
            ApplicantNotificationSmsGateway applicantNotificationSmsGateway,
            JobService jobService,
            AdminMailTemplateMapper adminMailTemplateMapper,
            NotificationProperties notificationProperties,
            PublicFormProtectionService protectionService,
            AdminActionLogService adminActionLogService
    ) {
        this.adminKeywordMatchTargetMapper = adminKeywordMatchTargetMapper;
        this.notificationApplicationMapper = notificationApplicationMapper;
        this.adminNotificationLogMapper = adminNotificationLogMapper;
        this.applicantNotificationMailGateway = applicantNotificationMailGateway;
        this.applicantNotificationSmsGateway = applicantNotificationSmsGateway;
        this.jobService = jobService;
        this.adminMailTemplateMapper = adminMailTemplateMapper;
        this.notificationProperties = notificationProperties;
        this.protectionService = protectionService;
        this.adminActionLogService = adminActionLogService;
    }

    public List<AdminNotificationLog> getNotificationLogs(Long documentSrl) {
        return adminNotificationLogMapper.findByDocumentSrl(documentSrl);
    }

    @Transactional("adminTransactionManager")
    public void sendEmailNotifications(Long documentSrl, Long matchJobId, AdminPrincipal principal, HttpServletRequest request) {
        JobDetail jobDetail = jobService.getJob(documentSrl);
        int sentCount = 0;
        for (AdminKeywordMatchTarget target : adminKeywordMatchTargetMapper.findByMatchJobId(matchJobId)) {
            if (!"Y".equals(target.getNotifyEmailYn())) {
                continue;
            }
            NotificationApplicationRecipient recipient = notificationApplicationMapper.findRecipientByApplicationId(target.getApplicationId());
            if (recipient == null) {
                continue;
            }
            String keywordSummary = trimSummary(target.getMatchedKeyword());
            if (adminNotificationLogMapper.countSuccessfulDuplicate(documentSrl, target.getApplicationId(), "EMAIL", keywordSummary) > 0) {
                insertLog(documentSrl, target.getApplicationId(), "EMAIL", recipient.getEmailAddressMasked(), keywordSummary, "SKIPPED_DUPLICATE", null);
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "EMAIL_SKIPPED_DUPLICATE", null, null);
                continue;
            }
            String email = decrypt(recipient.getEmailAddressEnc());
            if (email == null || email.isBlank()) {
                insertLog(documentSrl, target.getApplicationId(), "EMAIL", recipient.getEmailAddressMasked(), keywordSummary, "FAILED", "이메일 주소를 확인할 수 없습니다.");
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "EMAIL_FAILED", null, "이메일 주소를 확인할 수 없습니다.");
                continue;
            }
            try {
                NotificationTemplate notificationTemplate = resolveNotificationTemplate(jobDetail, recipient, keywordSummary);
                applicantNotificationMailGateway.dispatch(new NotificationEmailRequest(
                        email,
                        notificationTemplate.subject(),
                        notificationTemplate.body()
                ));
                LocalDateTime sentAt = LocalDateTime.now();
                insertLog(documentSrl, target.getApplicationId(), "EMAIL", recipient.getEmailAddressMasked(), keywordSummary, "SENT", null);
                SecondaryEmailResult secondaryResult = dispatchSecondaryEmailIfEnabled(
                        documentSrl,
                        target,
                        recipient,
                        email,
                        jobDetail,
                        keywordSummary
                );
                adminKeywordMatchTargetMapper.updateNotificationState(
                        target.getId(),
                        secondaryResult.notifyStatus(),
                        sentAt,
                        secondaryResult.failReason()
                );
                sentCount++;
            } catch (Exception ex) {
                String reason = trimFailureReason(ex.getMessage());
                insertLog(documentSrl, target.getApplicationId(), "EMAIL", recipient.getEmailAddressMasked(), keywordSummary, "FAILED", reason);
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "EMAIL_FAILED", null, reason);
            }
        }
        adminActionLogService.log(
                principal.getId(),
                "KEYWORD_NOTIFICATION_EMAIL",
                "JOB",
                String.valueOf(documentSrl),
                "Keyword notification email dispatch completed for match job #" + matchJobId + " with " + sentCount + " sends.",
                request
        );
    }

    @Transactional("adminTransactionManager")
    public void sendSmsNotifications(Long documentSrl, Long matchJobId, AdminPrincipal principal, HttpServletRequest request) {
        JobDetail jobDetail = jobService.getJob(documentSrl);
        int sentCount = 0;
        for (AdminKeywordMatchTarget target : adminKeywordMatchTargetMapper.findByMatchJobId(matchJobId)) {
            if (!"Y".equals(target.getNotifySmsYn())) {
                continue;
            }
            NotificationApplicationRecipient recipient = notificationApplicationMapper.findRecipientByApplicationId(target.getApplicationId());
            if (recipient == null) {
                continue;
            }
            String keywordSummary = trimSummary(target.getMatchedKeyword());
            if (adminNotificationLogMapper.countSuccessfulDuplicate(documentSrl, target.getApplicationId(), "SMS", keywordSummary) > 0) {
                insertLog(documentSrl, target.getApplicationId(), "SMS", recipient.getMobilePhoneMasked(), keywordSummary, "SKIPPED_DUPLICATE", null);
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "SMS_SKIPPED_DUPLICATE", null, null);
                continue;
            }
            String mobilePhone = decrypt(recipient.getMobilePhoneEnc());
            if (mobilePhone == null || mobilePhone.isBlank()) {
                insertLog(documentSrl, target.getApplicationId(), "SMS", recipient.getMobilePhoneMasked(), keywordSummary, "FAILED", "휴대전화 번호를 확인할 수 없습니다.");
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "SMS_FAILED", null, "휴대전화 번호를 확인할 수 없습니다.");
                continue;
            }
            try {
                applicantNotificationSmsGateway.dispatch(new NotificationSmsRequest(
                        mobilePhone,
                        buildSmsMessage(jobDetail, keywordSummary)
                ));
                LocalDateTime sentAt = LocalDateTime.now();
                insertLog(documentSrl, target.getApplicationId(), "SMS", recipient.getMobilePhoneMasked(), keywordSummary, "SENT", null);
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "SMS_SENT", sentAt, null);
                sentCount++;
            } catch (Exception ex) {
                String reason = trimFailureReason(ex.getMessage());
                insertLog(documentSrl, target.getApplicationId(), "SMS", recipient.getMobilePhoneMasked(), keywordSummary, "FAILED", reason);
                adminKeywordMatchTargetMapper.updateNotificationState(target.getId(), "SMS_FAILED", null, reason);
            }
        }
        adminActionLogService.log(
                principal.getId(),
                "KEYWORD_NOTIFICATION_SMS",
                "JOB",
                String.valueOf(documentSrl),
                "키워드 SMS 알림 발송 완료: 매칭 작업 #" + matchJobId + ", 발송 " + sentCount + "건",
                request
        );
    }

    private void insertLog(
            Long documentSrl,
            Long applicationId,
            String channelType,
            String targetAddressMasked,
            String keywordSummary,
            String sendStatus,
            String failReason
    ) {
        AdminNotificationLog log = new AdminNotificationLog();
        log.setDocumentSrl(documentSrl);
        log.setApplicationId(applicationId);
        log.setChannelType(channelType);
        log.setTargetAddressMasked(targetAddressMasked);
        log.setKeywordSummary(keywordSummary);
        log.setSendStatus(sendStatus);
        log.setFailReason(failReason);
        adminNotificationLogMapper.insert(log);
    }

    private SecondaryEmailResult dispatchSecondaryEmailIfEnabled(
            Long documentSrl,
            AdminKeywordMatchTarget target,
            NotificationApplicationRecipient recipient,
            String email,
            JobDetail jobDetail,
            String keywordSummary
    ) {
        if (!notificationProperties.isSecondaryEmailEnabled()) {
            return new SecondaryEmailResult("EMAIL_SENT", null);
        }
        if (adminNotificationLogMapper.countSuccessfulDuplicate(documentSrl, target.getApplicationId(), "EMAIL_SECONDARY", keywordSummary) > 0) {
            insertLog(documentSrl, target.getApplicationId(), "EMAIL_SECONDARY", recipient.getEmailAddressMasked(), keywordSummary, "SKIPPED_DUPLICATE", null);
            return new SecondaryEmailResult("EMAIL_SECONDARY_SKIPPED_DUPLICATE", null);
        }
        try {
            applicantNotificationMailGateway.dispatch(new NotificationEmailRequest(
                    email,
                    notificationProperties.getSecondaryEmailSubject(),
                    buildSecondaryEmailBody(jobDetail, recipient.getApplicantName(), keywordSummary)
            ));
            insertLog(documentSrl, target.getApplicationId(), "EMAIL_SECONDARY", recipient.getEmailAddressMasked(), keywordSummary, "SENT", null);
            return new SecondaryEmailResult("EMAIL_SECONDARY_SENT", null);
        } catch (Exception ex) {
            String reason = trimFailureReason(ex.getMessage());
            insertLog(documentSrl, target.getApplicationId(), "EMAIL_SECONDARY", recipient.getEmailAddressMasked(), keywordSummary, "FAILED", reason);
            return new SecondaryEmailResult("EMAIL_SECONDARY_FAILED", reason);
        }
    }

    private String buildEmailBody(JobDetail jobDetail, String applicantName, String keywordSummary) {
        Map<String, String> variables = notificationVariables(jobDetail, applicantName, keywordSummary);
        return renderTemplate(notificationProperties.getRecommendationEmailBody(), variables);
    }

    private String buildSecondaryEmailBody(JobDetail jobDetail, String applicantName, String keywordSummary) {
        Map<String, String> variables = notificationVariables(jobDetail, applicantName, keywordSummary);
        return renderTemplate("""
                안녕하세요 {{applicantName}}님,

                이전에 추천드린 일감을 다시 안내드립니다.
                추천 일감: {{jobTitle}}
                관련 키워드: {{keywordSummary}}
                신청 링크: {{applyUrl}}

                관심 없으시면 이 메시지는 무시하셔도 됩니다.
                """, variables);
    }

    private String buildSmsMessage(JobDetail jobDetail, String keywordSummary) {
        return renderTemplate(
                notificationProperties.getRecommendationSmsMessage(),
                notificationVariables(jobDetail, "", keywordSummary)
        );
    }

    private NotificationTemplate resolveNotificationTemplate(
            JobDetail jobDetail,
            NotificationApplicationRecipient recipient,
            String keywordSummary
    ) {
        Map<String, String> variables = notificationVariables(jobDetail, recipient.getApplicantName(), keywordSummary);
        AdminMailTemplate template = null;
        String templateName = notificationProperties.getRecommendationTemplateName();
        if (templateName != null && !templateName.isBlank()) {
            template = adminMailTemplateMapper.findActiveByName(templateName.trim());
        }
        if (template != null) {
            return new NotificationTemplate(
                    renderTemplate(template.getMailSubject(), variables),
                    renderTemplate(template.getMailBody(), variables)
            );
        }
        return new NotificationTemplate(
                renderTemplate(notificationProperties.getRecommendationEmailSubject(), variables),
                buildEmailBody(jobDetail, recipient.getApplicantName(), keywordSummary)
        );
    }

    private Map<String, String> notificationVariables(JobDetail jobDetail, String applicantName, String keywordSummary) {
        Map<String, String> variables = new LinkedHashMap<>();
        String name = applicantName == null || applicantName.isBlank() ? "지원자" : applicantName.trim();
        String applyUrl = trimTrailingSlash(notificationProperties.getBaseUrl()) + "/apply/" + jobDetail.getDocument().getDocumentSrl();
        variables.put("applicantName", name);
        variables.put("jobTitle", jobDetail.getDocument().getTitle());
        variables.put("documentSrl", String.valueOf(jobDetail.getDocument().getDocumentSrl()));
        variables.put("keywordSummary", keywordSummary);
        variables.put("applyUrl", applyUrl);
        return variables;
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private String trimTrailingSlash(String value) {
        String baseUrl = value == null || value.isBlank() ? "http://localhost:8082" : value.trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String trimSummary(String value) {
        if (value == null || value.isBlank()) {
            return "추천 일감";
        }
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    private String trimFailureReason(String value) {
        if (value == null || value.isBlank()) {
            return "알림 발송에 실패했습니다.";
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return protectionService.decrypt(value);
    }

    private record SecondaryEmailResult(String notifyStatus, String failReason) {
    }

    private record NotificationTemplate(String subject, String body) {
    }
}
