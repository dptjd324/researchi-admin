package com.researchi.admin.legacy.research.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import com.researchi.admin.legacy.mail.mapper.LegacyMailRuleMapper;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSupportService;
import com.researchi.admin.legacy.research.service.rule.LegacyResearchMailRuleService;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.mapper.AdminMailApplicationClaimMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyResearchMailRuleServiceTest {

    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private LegacyMailRuleMapper legacyMailRuleMapper;
    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminMailSendTargetMapper adminMailSendTargetMapper;
    @Mock
    private AdminMailApplicationClaimMapper adminMailApplicationClaimMapper;
    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private ResearchApplicationService researchApplicationService;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void saveMailRuleBuildsLegacyRuleWithoutChangingFacadeBehavior() {
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(new ResearchMaster());
        LegacyResearchMailRuleService service = new LegacyResearchMailRuleService(
                researchMasterService,
                legacyMailRuleMapper,
                mailSupport()
        );

        service.saveMailRule(46408L, 10, null, "Subject", "Body", MailAttachmentType.XLSX, true);

        ArgumentCaptor<LegacyMailRule> captor = ArgumentCaptor.forClass(LegacyMailRule.class);
        verify(legacyMailRuleMapper).upsert(captor.capture());
        LegacyMailRule rule = captor.getValue();
        assertThat(rule.getResearchNo()).isEqualTo(46408L);
        assertThat(rule.getThresholdCount()).isEqualTo(10);
        assertThat(rule.getDirectMailSubject()).isEqualTo("Subject");
        assertThat(rule.getDirectMailBody()).isEqualTo("Body");
        assertThat(rule.getAttachmentType()).isEqualTo("XLSX");
        assertThat(rule.getEnabledYn()).isEqualTo("Y");
    }

    private LegacyResearchMailSupportService mailSupport() {
        return new LegacyResearchMailSupportService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailApplicationClaimMapper,
                researchApplicationMapper,
                researchApplicationService,
                adminActionLogService
        );
    }
}
