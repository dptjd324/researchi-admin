package com.researchi.admin.legacy.research.service.rule;

import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import com.researchi.admin.legacy.mail.mapper.LegacyMailRuleMapper;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSupportService;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LegacyResearchMailRuleService {

    private static final String DEFAULT_ATTACHMENT_TYPE = "XLSX";
    private static final int MAX_MAIL_SUBJECT_LENGTH = 255;

    private final ResearchMasterService researchMasterService;
    private final LegacyMailRuleMapper legacyMailRuleMapper;
    private final LegacyResearchMailSupportService mailSupport;

    public LegacyResearchMailRuleService(
            ResearchMasterService researchMasterService,
            LegacyMailRuleMapper legacyMailRuleMapper,
            LegacyResearchMailSupportService mailSupport
    ) {
        this.researchMasterService = researchMasterService;
        this.legacyMailRuleMapper = legacyMailRuleMapper;
        this.mailSupport = mailSupport;
    }

    public LegacyMailRule getMailRule(Long researchNo) {
        LegacyMailRule rule = legacyMailRuleMapper.findByResearchNo(researchNo);
        if (rule != null) {
            return rule;
        }
        LegacyMailRule defaultRule = new LegacyMailRule();
        defaultRule.setResearchNo(researchNo);
        defaultRule.setThresholdCount(null);
        defaultRule.setAttachmentType(DEFAULT_ATTACHMENT_TYPE);
        defaultRule.setEnabledYn("N");
        return defaultRule;
    }

    public List<LegacyMailRule> getMailRuleItems(Long researchNo) {
        if (researchNo == null) {
            return List.of();
        }
        return legacyMailRuleMapper.findRuleItemsByResearchNo(researchNo);
    }

    @Transactional("adminTransactionManager")
    public void saveMailRule(
            Long researchNo,
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            boolean enabled
    ) {
        researchMasterService.getResearchMaster(researchNo);
        validateMailRule(thresholdCount, templateId, directMailSubject, directMailBody, enabled);
        legacyMailRuleMapper.upsert(buildMailRule(
                researchNo,
                thresholdCount,
                templateId,
                directMailSubject,
                directMailBody,
                attachmentType,
                true
        ));
    }

    @Transactional("adminTransactionManager")
    public void addMailRuleItem(
            Long researchNo,
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            boolean enabled
    ) {
        researchMasterService.getResearchMaster(researchNo);
        validateMailRule(thresholdCount, templateId, directMailSubject, directMailBody, true);
        legacyMailRuleMapper.insertRuleItem(buildMailRule(
                researchNo,
                thresholdCount,
                templateId,
                directMailSubject,
                directMailBody,
                attachmentType,
                true
        ));
    }

    @Transactional("adminTransactionManager")
    public void deleteMailRuleItem(Long researchNo, Long ruleId) {
        if (ruleId == null || legacyMailRuleMapper.deleteRuleItem(ruleId, researchNo) == 0) {
            throw new IllegalArgumentException("임계치 발송 설정을 찾을 수 없습니다.");
        }
    }

    @Transactional("adminTransactionManager")
    public void cancelMailRule(Long researchNo) {
        if (legacyMailRuleMapper.disableByResearchNo(researchNo) == 0) {
            throw new IllegalArgumentException("임계치 발송 설정을 찾을 수 없습니다.");
        }
    }

    private void validateMailRule(
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            boolean thresholdCountRequired
    ) {
        if (thresholdCountRequired && (thresholdCount == null || thresholdCount < 1)) {
            throw new IllegalArgumentException("임계치 인원은 1명 이상이어야 합니다.");
        }
        if (templateId == null && mailSupport.trimToNull(directMailSubject) == null) {
            throw new IllegalArgumentException("메일 제목을 입력해 주세요.");
        }
    }

    private LegacyMailRule buildMailRule(
            Long researchNo,
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            boolean enabled
    ) {
        LegacyMailRule rule = new LegacyMailRule();
        rule.setResearchNo(researchNo);
        rule.setThresholdCount(thresholdCount);
        rule.setTemplateId(templateId);
        rule.setDirectMailSubject(trimMailSubject(directMailSubject));
        rule.setDirectMailBody(mailSupport.trimToNull(directMailBody));
        rule.setAttachmentType(attachmentType.name());
        rule.setEnabledYn(enabled ? "Y" : "N");
        return rule;
    }

    private String trimMailSubject(String value) {
        String subject = mailSupport.trimToNull(value);
        if (subject == null || subject.length() <= MAX_MAIL_SUBJECT_LENGTH) {
            return subject;
        }
        return subject.substring(0, MAX_MAIL_SUBJECT_LENGTH);
    }
}
