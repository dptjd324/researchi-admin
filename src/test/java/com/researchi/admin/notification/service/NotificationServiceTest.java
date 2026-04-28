package com.researchi.admin.notification.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.matching.domain.AdminKeywordMatchTarget;
import com.researchi.admin.matching.mapper.AdminKeywordMatchTargetMapper;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.notification.domain.NotificationApplicationRecipient;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.notification.mapper.NotificationApplicationMapper;
import com.researchi.admin.publicform.config.PublicFormProperties;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper;
    @Mock
    private NotificationApplicationMapper notificationApplicationMapper;
    @Mock
    private AdminNotificationLogMapper adminNotificationLogMapper;
    @Mock
    private ApplicantNotificationMailGateway applicantNotificationMailGateway;
    @Mock
    private ApplicantNotificationSmsGateway applicantNotificationSmsGateway;
    @Mock
    private JobService jobService;
    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void sendEmailNotificationsSkipsSuccessfulDuplicates() throws Exception {
        PublicFormProperties publicFormProperties = new PublicFormProperties();
        publicFormProperties.setEncryptionKey("phase10-test-key");
        publicFormProperties.setCaptchaEnabled(false);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBaseUrl("http://localhost:8081");
        NotificationService notificationService = new NotificationService(
                adminKeywordMatchTargetMapper,
                notificationApplicationMapper,
                adminNotificationLogMapper,
                applicantNotificationMailGateway,
                applicantNotificationSmsGateway,
                jobService,
                adminMailTemplateMapper,
                notificationProperties,
                new PublicFormProtectionService(publicFormProperties),
                adminActionLogService
        );

        AdminKeywordMatchTarget target = new AdminKeywordMatchTarget();
        target.setId(55L);
        target.setApplicationId(101L);
        target.setMatchedKeyword("survey");
        target.setNotifyEmailYn("Y");
        when(adminKeywordMatchTargetMapper.findByMatchJobId(44L)).thenReturn(List.of(target));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(9L, 101L, "EMAIL", "survey")).thenReturn(1);
        NotificationApplicationRecipient recipient = new NotificationApplicationRecipient();
        recipient.setApplicationId(101L);
        recipient.setEmailAddressMasked("applicant@example.com");
        when(notificationApplicationMapper.findRecipientByApplicationId(101L)).thenReturn(recipient);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));

        notificationService.sendEmailNotifications(
                9L,
                44L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(applicantNotificationMailGateway, never()).dispatch(any());
        verify(notificationApplicationMapper).findRecipientByApplicationId(101L);
        verify(adminKeywordMatchTargetMapper).updateNotificationState(55L, "EMAIL_SKIPPED_DUPLICATE", null, null);
        verify(adminNotificationLogMapper).insert(any());
    }

    @Test
    void sendSmsNotificationsDecryptsPhoneAndDispatches() throws Exception {
        PublicFormProperties publicFormProperties = new PublicFormProperties();
        publicFormProperties.setEncryptionKey("phase10-test-key");
        publicFormProperties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(publicFormProperties);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBaseUrl("http://localhost:8081");
        NotificationService notificationService = new NotificationService(
                adminKeywordMatchTargetMapper,
                notificationApplicationMapper,
                adminNotificationLogMapper,
                applicantNotificationMailGateway,
                applicantNotificationSmsGateway,
                jobService,
                adminMailTemplateMapper,
                notificationProperties,
                protectionService,
                adminActionLogService
        );

        AdminKeywordMatchTarget target = new AdminKeywordMatchTarget();
        target.setId(56L);
        target.setApplicationId(102L);
        target.setMatchedKeyword("panel");
        target.setNotifySmsYn("Y");
        when(adminKeywordMatchTargetMapper.findByMatchJobId(45L)).thenReturn(List.of(target));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(9L, 102L, "SMS", "panel")).thenReturn(0);
        NotificationApplicationRecipient recipient = new NotificationApplicationRecipient();
        recipient.setApplicationId(102L);
        recipient.setMobilePhoneEnc(protectionService.encrypt("01012345678"));
        recipient.setMobilePhoneMasked("01012345678");
        when(notificationApplicationMapper.findRecipientByApplicationId(102L)).thenReturn(recipient);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));

        notificationService.sendSmsNotifications(
                9L,
                45L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        ArgumentCaptor<com.researchi.admin.notification.domain.NotificationSmsRequest> requestCaptor =
                ArgumentCaptor.forClass(com.researchi.admin.notification.domain.NotificationSmsRequest.class);
        verify(applicantNotificationSmsGateway).dispatch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().recipient()).isEqualTo("01012345678");
        verify(adminKeywordMatchTargetMapper).updateNotificationState(eq(56L), eq("SMS_SENT"), any(), eq(null));
    }

    @Test
    void sendEmailNotificationsAutomaticallyDispatchesSecondaryEmailWhenEnabled() throws Exception {
        PublicFormProperties publicFormProperties = new PublicFormProperties();
        publicFormProperties.setEncryptionKey("phase10-test-key");
        publicFormProperties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(publicFormProperties);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBaseUrl("http://localhost:8081");
        notificationProperties.setSecondaryEmailEnabled(true);
        notificationProperties.setSecondaryEmailSubject("[Researchi] Reminder for your matched job");
        NotificationService notificationService = new NotificationService(
                adminKeywordMatchTargetMapper,
                notificationApplicationMapper,
                adminNotificationLogMapper,
                applicantNotificationMailGateway,
                applicantNotificationSmsGateway,
                jobService,
                adminMailTemplateMapper,
                notificationProperties,
                protectionService,
                adminActionLogService
        );

        AdminKeywordMatchTarget target = new AdminKeywordMatchTarget();
        target.setId(57L);
        target.setApplicationId(103L);
        target.setMatchedKeyword("survey, panel");
        target.setNotifyEmailYn("Y");
        when(adminKeywordMatchTargetMapper.findByMatchJobId(46L)).thenReturn(List.of(target));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(9L, 103L, "EMAIL", "survey, panel")).thenReturn(0);
        when(adminNotificationLogMapper.countSuccessfulDuplicate(9L, 103L, "EMAIL_SECONDARY", "survey, panel")).thenReturn(0);
        NotificationApplicationRecipient recipient = new NotificationApplicationRecipient();
        recipient.setApplicationId(103L);
        recipient.setApplicantName("Kim");
        recipient.setEmailAddressEnc(protectionService.encrypt("kim@example.com"));
        recipient.setEmailAddressMasked("kim@example.com");
        when(notificationApplicationMapper.findRecipientByApplicationId(103L)).thenReturn(recipient);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));

        notificationService.sendEmailNotifications(
                9L,
                46L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        ArgumentCaptor<com.researchi.admin.notification.domain.NotificationEmailRequest> requestCaptor =
                ArgumentCaptor.forClass(com.researchi.admin.notification.domain.NotificationEmailRequest.class);
        verify(applicantNotificationMailGateway, org.mockito.Mockito.times(2)).dispatch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).hasSize(2);
        assertThat(requestCaptor.getAllValues().get(0).subject()).isEqualTo("[Researchi] 새로운 일감을 추천드립니다");
        assertThat(requestCaptor.getAllValues().get(0).body()).contains("새로 등록된 일감");
        assertThat(requestCaptor.getAllValues().get(1).subject()).isEqualTo("[Researchi] Reminder for your matched job");
        verify(adminKeywordMatchTargetMapper).updateNotificationState(eq(57L), eq("EMAIL_SECONDARY_SENT"), any(), eq(null));
    }

    @Test
    void sendEmailNotificationsUsesManagedRecommendationTemplateWhenPresent() throws Exception {
        PublicFormProperties publicFormProperties = new PublicFormProperties();
        publicFormProperties.setEncryptionKey("phase10-test-key");
        publicFormProperties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(publicFormProperties);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBaseUrl("http://localhost:8082");
        NotificationService notificationService = new NotificationService(
                adminKeywordMatchTargetMapper,
                notificationApplicationMapper,
                adminNotificationLogMapper,
                applicantNotificationMailGateway,
                applicantNotificationSmsGateway,
                jobService,
                adminMailTemplateMapper,
                notificationProperties,
                protectionService,
                adminActionLogService
        );

        AdminKeywordMatchTarget target = new AdminKeywordMatchTarget();
        target.setId(58L);
        target.setApplicationId(104L);
        target.setMatchedKeyword("커피");
        target.setNotifyEmailYn("Y");
        when(adminKeywordMatchTargetMapper.findByMatchJobId(47L)).thenReturn(List.of(target));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(9L, 104L, "EMAIL", "커피")).thenReturn(0);
        NotificationApplicationRecipient recipient = new NotificationApplicationRecipient();
        recipient.setApplicationId(104L);
        recipient.setApplicantName("이예성");
        recipient.setEmailAddressEnc(protectionService.encrypt("lee@example.com"));
        recipient.setEmailAddressMasked("lee@example.com");
        when(notificationApplicationMapper.findRecipientByApplicationId(104L)).thenReturn(recipient);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        AdminMailTemplate template = new AdminMailTemplate();
        template.setMailSubject("{{applicantName}}님께 추천드리는 새 일감");
        template.setMailBody("{{jobTitle}} / {{keywordSummary}} / {{applyUrl}}");
        when(adminMailTemplateMapper.findActiveByName("일감 추천 알림")).thenReturn(template);

        notificationService.sendEmailNotifications(
                9L,
                47L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        ArgumentCaptor<com.researchi.admin.notification.domain.NotificationEmailRequest> requestCaptor =
                ArgumentCaptor.forClass(com.researchi.admin.notification.domain.NotificationEmailRequest.class);
        verify(applicantNotificationMailGateway).dispatch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().subject()).isEqualTo("이예성님께 추천드리는 새 일감");
        assertThat(requestCaptor.getValue().body()).contains("Keyword Job", "커피", "http://localhost:8082/apply/9");
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Keyword Job");
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }
}
