package com.researchi.admin.legacy.application.service;

import com.researchi.admin.legacy.application.domain.LegacyApplicationConsent;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationConsentMapper;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LegacyApplicationConsentService {

    public static final String CONSENT_VERSION = "2026-07-13-v1";
    public static final String NOTICE_SNAPSHOT = """
            리서치아이 개인정보 수집·이용 및 리서치 안내 수신 동의
            필수 정보 보유기간: 해당 리서치 종료 후 2년
            향후 모집 정보 보유기간: 동의일로부터 2년 또는 동의 철회일까지
            선택 동의: 향후 리서치 모집·추천, SMS 수신, 이메일 수신
            동의 철회: spirit2@naver.com
            """;

    private final LegacyApplicationConsentMapper consentMapper;

    public LegacyApplicationConsentService(LegacyApplicationConsentMapper consentMapper) {
        this.consentMapper = consentMapper;
    }

    @Transactional("adminTransactionManager")
    public void recordSubmissionConsent(Long researchNo, Long researchAppSeq, PublicApplicationForm form) {
        LocalDateTime consentedAt = LocalDateTime.now();
        LegacyApplicationConsent consent = new LegacyApplicationConsent();
        consent.setResearchNo(researchNo);
        consent.setResearchAppSeq(researchAppSeq);
        consent.setRequiredPrivacyYn(yn(form.getProvideYn()));
        consent.setFutureRecruitmentYn(yn(form.getFutureRecruitmentYn()));
        consent.setSmsYn(yn(form.getNotifySmsYn()));
        consent.setEmailYn(yn(form.getNotifyEmailYn()));
        consent.setConsentVersion(CONSENT_VERSION);
        consent.setNoticeSnapshot(NOTICE_SNAPSHOT);
        consent.setConsentedAt(consentedAt);
        consent.setFutureConsentExpiresAt(Boolean.TRUE.equals(form.getFutureRecruitmentYn())
                ? consentedAt.plusYears(2)
                : null);
        consentMapper.insert(consent);
    }

    @Transactional(transactionManager = "adminTransactionManager", readOnly = true)
    public List<ResearchApplication> filterActiveFutureRecruitment(List<ResearchApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return List.of();
        }
        List<Long> applicationSeqs = applications.stream()
                .map(ResearchApplication::getResearchAppSeq)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (applicationSeqs.isEmpty()) {
            return List.of();
        }
        Set<Long> activeSeqs = new HashSet<>(
                consentMapper.findActiveFutureRecruitmentApplicationSeqs(applicationSeqs, LocalDateTime.now())
        );
        return applications.stream()
                .filter(application -> activeSeqs.contains(application.getResearchAppSeq()))
                .toList();
    }

    @Transactional(transactionManager = "adminTransactionManager", readOnly = true)
    public boolean allowsSms(Long researchNo, Long researchAppSeq) {
        LegacyApplicationConsent consent = consentMapper.findByApplication(researchNo, researchAppSeq);
        return consent != null && consent.allowsSmsAt(LocalDateTime.now());
    }

    @Transactional(transactionManager = "adminTransactionManager", readOnly = true)
    public boolean allowsEmail(Long researchNo, Long researchAppSeq) {
        LegacyApplicationConsent consent = consentMapper.findByApplication(researchNo, researchAppSeq);
        return consent != null && consent.allowsEmailAt(LocalDateTime.now());
    }

    private String yn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }
}
