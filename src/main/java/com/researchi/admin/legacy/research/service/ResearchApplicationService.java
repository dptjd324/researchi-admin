package com.researchi.admin.legacy.research.service;

import com.researchi.admin.legacy.application.support.ApplicationFormNoticeItem;
import com.researchi.admin.legacy.application.support.ApplicationFormNoticeParser;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.service.LegacyApplicationExtraAnswerFormatter;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.research.domain.ApplicantResearchHistoryItem;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchApplicationDuplicateGroup;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.mapper.ResearchMasterMapper;
import com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm;
import com.researchi.admin.legacy.revision.service.LegacyRevisionLogService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResearchApplicationService {

    private final ResearchApplicationMapper researchApplicationMapper;
    private final LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    private final LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    private final ResearchMasterMapper researchMasterMapper;
    private final LegacyRevisionLogService legacyRevisionLogService;

    public ResearchApplicationService(
            ResearchApplicationMapper researchApplicationMapper,
            LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper,
            LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper,
            ResearchMasterMapper researchMasterMapper,
            LegacyRevisionLogService legacyRevisionLogService
    ) {
        this.researchApplicationMapper = researchApplicationMapper;
        this.legacyApplicationExtraAnswerMapper = legacyApplicationExtraAnswerMapper;
        this.legacyApplicationSearchIndexMapper = legacyApplicationSearchIndexMapper;
        this.researchMasterMapper = researchMasterMapper;
        this.legacyRevisionLogService = legacyRevisionLogService;
    }

    public List<ResearchApplication> getApplicationPage(Long researchNo, ResearchApplicationSearchForm searchForm, int limit, int offset) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        ResearchApplicationSearchForm normalized = normalize(searchForm);
        if (shouldUseSearchIndex(researchNo, normalized)) {
            return enrichResearchTitles(legacyApplicationSearchIndexMapper.findPage(
                    normalized.isAllResearch() ? null : researchNo,
                    normalized,
                    safeLimit(limit),
                    safeOffset(offset)
            ));
        }
        if (normalized.isAllResearch()) {
            if (isSearchIndexReady()) {
                return enrichResearchTitles(legacyApplicationSearchIndexMapper.findPage(null, normalized, safeLimit(limit), safeOffset(offset)));
            }
            return enrichResearchTitles(researchApplicationMapper.findPage(normalized, safeLimit(limit), safeOffset(offset)));
        }
        return enrichResearchTitles(researchApplicationMapper.findPageByResearchNo(researchNo, normalized, safeLimit(limit), safeOffset(offset)));
    }

    public List<ResearchApplication> getApplications(Long researchNo, ResearchApplicationSearchForm searchForm) {
        int totalCount = countApplications(researchNo, searchForm);
        if (totalCount < 1) {
            return List.of();
        }
        return getApplicationPage(researchNo, searchForm, totalCount, 0);
    }

    public ResearchApplication getApplication(Long researchNo, Long researchAppSeq) {
        if (researchNo == null || researchAppSeq == null) {
            throw new IllegalArgumentException("researchNo and researchAppSeq are required.");
        }
        ResearchApplication application = researchApplicationMapper.findByResearchNoAndSeq(researchNo, researchAppSeq);
        if (application == null) {
            throw new IllegalArgumentException("Research application not found. researchNo=" + researchNo + ", researchAppSeq=" + researchAppSeq);
        }
        enrichEmail(application);
        return application;
    }

    public String getFormattedAdditionalAnswers(Long researchNo, Long researchAppSeq) {
        return LegacyApplicationExtraAnswerFormatter.format(
                legacyApplicationExtraAnswerMapper.findByResearchApplication(researchNo, researchAppSeq)
        );
    }

    public String getFormattedAdditionalAnswers(Long researchNo, Long researchAppSeq, ResearchMaster researchMaster) {
        return LegacyApplicationExtraAnswerFormatter.format(
                legacyApplicationExtraAnswerMapper.findByResearchApplication(researchNo, researchAppSeq),
                singleAdditionalGroup(researchMaster)
        );
    }

    public List<Long> getApplicationSeqsByAdditionalAnswerTerms(List<String> terms, int limit) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        List<Long> researchAppSeqs = legacyApplicationExtraAnswerMapper.findResearchAppSeqsByAnswerTerms(
                terms,
                safeIndexLimit(limit)
        );
        return researchAppSeqs == null ? List.of() : researchAppSeqs;
    }

    public List<ApplicantResearchHistoryItem> getApplicantResearchHistory(ResearchApplication application) {
        if (application == null) {
            return List.of();
        }
        boolean hasIdentity = hasText(application.getAppName())
                && hasText(application.getAppBirth())
                && (hasText(application.getAppHphone()) || hasText(application.getAppTele()));
        if (!hasIdentity) {
            return List.of();
        }
        return researchApplicationMapper.findApplicantResearchHistory(
                trimToNull(application.getAppName()),
                trimToNull(application.getAppBirth()),
                trimToNull(application.getAppHphone()),
                trimToNull(application.getAppTele())
        );
    }

    private String singleAdditionalGroup(ResearchMaster researchMaster) {
        if (researchMaster == null) {
            return null;
        }
        List<String> groups = ApplicationFormNoticeParser.parseDetails(researchMaster.getAddComment()).stream()
                .map(ApplicationFormNoticeItem::groupLabel)
                .filter(group -> group != null && !group.isBlank())
                .distinct()
                .toList();
        return groups.size() == 1 ? groups.get(0) : null;
    }

    public List<ResearchApplication> getUnprovidedApplications(Long researchNo) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        completeProvisionForBlacklistedUnprovided(researchNo, null);
        return researchApplicationMapper.findUnprovidedByResearchNo(researchNo);
    }

    public List<ResearchApplicationDuplicateGroup> getDuplicateGroups(Long researchNo) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        return researchApplicationMapper.findDuplicateGroupsByResearchNo(researchNo);
    }

    public List<ResearchApplication> getAllApplications(Long researchNo) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        return researchApplicationMapper.findAllByResearchNo(researchNo);
    }

    public List<ResearchApplication> getAllApplications() {
        return researchApplicationMapper.findAll();
    }

    public List<ResearchApplication> getApplicationIndexPage(int limit, int offset) {
        return enrichResearchTitles(researchApplicationMapper.findAllPage(safeIndexLimit(limit), safeOffset(offset)));
    }

    public List<ResearchApplication> getMatchingIndexCandidatePage(List<String> includeKeywords, List<String> excludeKeywords, int limit, int offset) {
        return enrichEmails(researchApplicationMapper.findMatchingIndexCandidatePage(
                LegacyMatchingSearchCondition.empty(),
                safeIndexLimit(limit),
                safeOffset(offset)
        ));
    }

    public List<ResearchApplication> getMatchingIndexCandidatePage(LegacyMatchingSearchCondition condition, int limit, int offset) {
        return enrichEmails(researchApplicationMapper.findMatchingIndexCandidatePage(
                condition == null ? LegacyMatchingSearchCondition.empty() : condition,
                safeIndexLimit(limit),
                safeOffset(offset)
        ));
    }

    public void updateProvideYn(Long researchNo, Long researchAppSeq, String provideYn, Long changedBy) {
        String normalizedProvideYn = normalizeProvideYn(provideYn);
        ResearchApplication before = getApplication(researchNo, researchAppSeq);
        if (normalizedProvideYn.equalsIgnoreCase(before.getProvideYn())) {
            return;
        }
        legacyRevisionLogService.backupBeforeUpdate(
                "TB_RESEARCH_APP",
                researchNo + ":" + researchAppSeq,
                before,
                changedBy
        );
        int updated = researchApplicationMapper.updateProvideYn(researchNo, researchAppSeq, normalizedProvideYn);
        if (updated < 1) {
            throw new IllegalStateException("Failed to update research application provideYn. researchNo=" + researchNo + ", researchAppSeq=" + researchAppSeq);
        }
        refreshSearchIndex(researchNo, researchAppSeq);
    }

    public int completeProvisionForUnprovided(Long researchNo, Long changedBy) {
        completeProvisionForBlacklistedUnprovided(researchNo, changedBy);
        List<ResearchApplication> applications = getUnprovidedApplications(researchNo);
        Set<Long> researchAppSeqs = new LinkedHashSet<>();
        for (ResearchApplication application : applications) {
            if (application.getResearchAppSeq() != null) {
                researchAppSeqs.add(application.getResearchAppSeq());
            }
        }
        int updatedCount = 0;
        for (Long researchAppSeq : researchAppSeqs) {
            updateProvideYn(researchNo, researchAppSeq, "Y", changedBy);
            updatedCount++;
        }
        return updatedCount;
    }

    public int countApplications(Long researchNo, ResearchApplicationSearchForm searchForm) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        ResearchApplicationSearchForm normalized = normalize(searchForm);
        if (shouldUseSearchIndex(researchNo, normalized)) {
            return legacyApplicationSearchIndexMapper.count(normalized.isAllResearch() ? null : researchNo, normalized);
        }
        return normalized.isAllResearch()
                ? countAllResearchApplications(normalized)
                : researchApplicationMapper.countByResearchNo(researchNo, normalized);
    }

    public void refreshSearchIndex(Long researchNo, Long researchAppSeq) {
        if (researchNo == null || researchAppSeq == null) {
            return;
        }
        ResearchApplication application = researchApplicationMapper.findByResearchNoAndSeq(researchNo, researchAppSeq);
        if (application != null) {
            legacyApplicationSearchIndexMapper.upsert(application);
        }
    }

    private int countAllResearchApplications(ResearchApplicationSearchForm normalized) {
        if (isSearchIndexReady()) {
            return legacyApplicationSearchIndexMapper.count(null, normalized);
        }
        return researchApplicationMapper.count(normalized);
    }

    private boolean shouldUseSearchIndex(Long researchNo, ResearchApplicationSearchForm normalized) {
        return researchNo != null && normalized != null && trimToNull(normalized.getAppEmail()) != null;
    }

    private List<ResearchApplication> enrichResearchTitles(List<ResearchApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return applications;
        }
        List<Long> researchNos = applications.stream()
                .map(ResearchApplication::getResearchNo)
                .filter(no -> no != null)
                .distinct()
                .toList();
        if (researchNos.isEmpty()) {
            return applications;
        }
        Map<Long, String> titlesByResearchNo = researchMasterMapper.findByResearchNos(researchNos).stream()
                .filter(research -> research.getResearchNo() != null && research.getResearchTitle() != null)
                .collect(Collectors.toMap(ResearchMaster::getResearchNo, ResearchMaster::getResearchTitle, (first, second) -> first));
        for (ResearchApplication application : applications) {
            application.setResearchTitle(titlesByResearchNo.get(application.getResearchNo()));
        }
        return applications;
    }

    private void enrichEmail(ResearchApplication application) {
        if (application == null || application.getResearchNo() == null || application.getResearchAppSeq() == null) {
            return;
        }
        ResearchApplication indexed = legacyApplicationSearchIndexMapper.findByResearchNoAndSeq(
                application.getResearchNo(),
                application.getResearchAppSeq()
        );
        if (indexed != null) {
            application.setAppEmail(indexed.getAppEmail());
        }
    }

    private List<ResearchApplication> enrichEmails(List<ResearchApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return applications;
        }
        for (ResearchApplication application : applications) {
            enrichEmail(application);
        }
        return applications;
    }

    private boolean isSearchIndexReady() {
        try {
            return legacyApplicationSearchIndexMapper.isReady();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public int countAllApplications(Long researchNo) {
        if (researchNo == null) {
            return 0;
        }
        return researchApplicationMapper.countAllByResearchNo(researchNo);
    }

    public int countUnprovidedApplications(Long researchNo) {
        if (researchNo == null) {
            return 0;
        }
        completeProvisionForBlacklistedUnprovided(researchNo, null);
        return researchApplicationMapper.countUnprovidedByResearchNo(researchNo);
    }

    public int completeProvisionForBlacklistedUnprovided(Long researchNo, Long changedBy) {
        if (researchNo == null) {
            return 0;
        }
        List<Long> researchAppSeqs = researchApplicationMapper.findBlacklistedUnprovidedSeqsByResearchNo(researchNo);
        if (researchAppSeqs == null || researchAppSeqs.isEmpty()) {
            return 0;
        }
        Set<Long> uniqueSeqs = new LinkedHashSet<>(researchAppSeqs);
        int updatedCount = 0;
        for (Long researchAppSeq : uniqueSeqs) {
            updateProvideYn(researchNo, researchAppSeq, "Y", changedBy);
            updatedCount++;
        }
        return updatedCount;
    }

    private ResearchApplicationSearchForm normalize(ResearchApplicationSearchForm searchForm) {
        ResearchApplicationSearchForm normalized = searchForm == null ? new ResearchApplicationSearchForm() : searchForm;
        normalized.setAppName(trimToNull(normalized.getAppName()));
        normalized.setAppSex(trimToNull(normalized.getAppSex()));
        normalized.setAppBirth(trimToNull(normalized.getAppBirth()));
        normalized.setAppAge(trimToNull(normalized.getAppAge()));
        normalized.setAppJob(trimToNull(normalized.getAppJob()));
        normalized.setAppCompany(trimToNull(normalized.getAppCompany()));
        normalized.setAppHphone(trimToNull(normalized.getAppHphone()));
        normalized.setAppTele(trimToNull(normalized.getAppTele()));
        normalized.setAppEmail(trimToNull(normalized.getAppEmail()));
        normalized.setAppAddr(trimToNull(normalized.getAppAddr()));
        normalized.setAddComment(trimToNull(normalized.getAddComment()));
        normalized.setAttendResearch(trimToNull(normalized.getAttendResearch()));
        normalized.setProvideYn(trimToNull(normalized.getProvideYn()));
        normalized.setViewMode(normalizeViewMode(normalized.getViewMode()));
        if ("PROVIDE".equals(normalized.getViewMode())) {
            normalized.setProvideYn("N");
        } else if ("ALL".equals(normalized.getViewMode())) {
            normalized.setProvideYn(null);
        }
        return normalized;
    }

    private String normalizeViewMode(String value) {
        String normalized = trimToNull(value);
        if ("PROVIDE".equals(normalized)) {
            return "PROVIDE";
        }
        return "ALL";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private String normalizeProvideYn(String value) {
        String normalized = trimToNull(value);
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException("provideYn must be Y or N.");
        }
        return normalized;
    }

    private int safeLimit(int limit) {
        if (limit < 1) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private int safeOffset(int offset) {
        return Math.max(offset, 0);
    }

    private int safeIndexLimit(int limit) {
        if (limit < 1) {
            return 1000;
        }
        return Math.min(limit, 10000);
    }
}
