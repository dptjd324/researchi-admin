package com.researchi.admin.legacy.research.service;

import com.researchi.admin.common.support.PhoneNumberFormatter;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchMasterMapper;
import com.researchi.admin.legacy.revision.service.LegacyRevisionLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResearchMasterService {

    private final ResearchMasterMapper researchMasterMapper;
    private final LegacyRevisionLogService legacyRevisionLogService;
    private final ResearchVisibilityService researchVisibilityService;

    public ResearchMasterService(
            ResearchMasterMapper researchMasterMapper,
            LegacyRevisionLogService legacyRevisionLogService,
            ResearchVisibilityService researchVisibilityService
    ) {
        this.researchMasterMapper = researchMasterMapper;
        this.legacyRevisionLogService = legacyRevisionLogService;
        this.researchVisibilityService = researchVisibilityService;
    }

    public ResearchMaster getResearchMaster(Long researchNo) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        ResearchMaster researchMaster = researchMasterMapper.findByResearchNo(researchNo);
        if (researchMaster == null) {
            throw new IllegalArgumentException("Research master not found. researchNo=" + researchNo);
        }
        return researchMaster;
    }

    public List<ResearchMaster> getResearchMasterPage(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            int limit,
            int offset
    ) {
        return getResearchMasterPage(keyword, registStart, registEnd, title, companyName, serverName, false, null, null, limit, offset);
    }

    public List<ResearchMaster> getResearchMasterPage(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            boolean hiddenOnly,
            String sort,
            String direction,
            int limit,
            int offset
    ) {
        return researchMasterMapper.findPage(
                trimToNull(keyword),
                normalizeDateForSearch(registStart),
                normalizeDateForSearch(registEnd),
                normalizeTitleForSearch(title),
                trimToNull(companyName),
                trimToNull(serverName),
                researchVisibilityService.getHiddenResearchNos(),
                hiddenOnly,
                normalizeSort(sort),
                normalizeSortDirection(direction),
                safeLimit(limit),
                safeOffset(offset)
        );
    }

    public List<ResearchMaster> getResearchMasterPage(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            boolean hiddenOnly,
            int limit,
            int offset
    ) {
        return getResearchMasterPage(keyword, registStart, registEnd, title, companyName, serverName, hiddenOnly, null, null, limit, offset);
    }

    public int countResearchMasters(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName
    ) {
        return countResearchMasters(keyword, registStart, registEnd, title, companyName, serverName, false);
    }

    public int countResearchMasters(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            boolean hiddenOnly
    ) {
        return countResearchMasters(
                keyword,
                registStart,
                registEnd,
                title,
                companyName,
                serverName,
                hiddenOnly,
                null
        );
    }

    public int countResearchMasters(
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            boolean hiddenOnly,
            String sort
    ) {
        return researchMasterMapper.count(
                trimToNull(keyword),
                normalizeDateForSearch(registStart),
                normalizeDateForSearch(registEnd),
                normalizeTitleForSearch(title),
                trimToNull(companyName),
                trimToNull(serverName),
                researchVisibilityService.getHiddenResearchNos(),
                hiddenOnly,
                normalizeSort(sort)
        );
    }

    public boolean isHidden(Long researchNo) {
        return researchVisibilityService.isHidden(researchNo);
    }

    public void assertNotHidden(Long researchNo) {
        if (isHidden(researchNo)) {
            throw new IllegalStateException("숨김 처리된 공고에서는 사용할 수 없는 기능입니다.");
        }
    }

    public void hideResearchMaster(Long researchNo, Long hiddenBy) {
        getResearchMaster(researchNo);
        researchVisibilityService.hide(researchNo, hiddenBy);
    }

    public void restoreResearchMaster(Long researchNo, Long restoredBy) {
        getResearchMaster(researchNo);
        researchVisibilityService.restore(researchNo, restoredBy);
    }

    public void updateResearchMaster(ResearchMaster researchMaster, Long changedBy) {
        if (researchMaster == null || researchMaster.getResearchNo() == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        ResearchMaster before = getResearchMaster(researchMaster.getResearchNo());
        researchMaster.setCloseDate(requireDateForSave(researchMaster.getCloseDate()));
        researchMaster.setContactNo(PhoneNumberFormatter.formatForDisplay(researchMaster.getContactNo()));
        legacyRevisionLogService.backupBeforeUpdate(
                "TB_RESEARCH_MST",
                String.valueOf(researchMaster.getResearchNo()),
                before,
                changedBy
        );
        int updated = researchMasterMapper.update(researchMaster);
        if (updated != 1) {
            throw new IllegalStateException("Failed to update research master. researchNo=" + researchMaster.getResearchNo());
        }
    }

    public Long createResearchMaster(ResearchMaster researchMaster) {
        if (researchMaster == null) {
            throw new IllegalArgumentException("research master is required.");
        }
        Long researchNo = researchMasterMapper.findNextResearchNo();
        researchMaster.setResearchNo(researchNo);
        researchMaster.setCalculateYn(defaultCalculateYn(researchMaster.getCalculateYn()));
        researchMaster.setCloseDate(requireDateForSave(researchMaster.getCloseDate()));
        researchMaster.setContactNo(PhoneNumberFormatter.formatForDisplay(researchMaster.getContactNo()));
        researchMasterMapper.insert(researchMaster);
        return researchNo;
    }

    public List<Long> getClosedResearchNosBefore(LocalDate closeDate) {
        LocalDate cutoff = closeDate == null ? LocalDate.now() : closeDate;
        return researchMasterMapper.findClosedResearchNosBefore(cutoff.format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDateForSearch(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return null;
        }
        return digits.substring(0, 8);
    }

    private String normalizeDateForSave(String value) {
        return normalizeDateForSearch(value);
    }

    private String requireDateForSave(String value) {
        String normalized = normalizeDateForSave(value);
        if (normalized == null) {
            throw new IllegalArgumentException("closeDate is required.");
        }
        return normalized;
    }

    private String defaultCalculateYn(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "N" : normalized;
    }

    private String normalizeTitleForSearch(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if ("좌담회/설문".equals(trimmed) || "좌담회".equals(trimmed) || "설문".equals(trimmed)) {
            return null;
        }
        return trimmed;
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

    private String normalizeSortDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    private String normalizeSort(String sort) {
        if ("appNewCnt".equals(sort) || "closeDate".equals(sort) || "registDt".equals(sort)) {
            return sort;
        }
        return null;
    }
}
