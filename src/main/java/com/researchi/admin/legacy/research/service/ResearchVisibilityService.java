package com.researchi.admin.legacy.research.service;

import com.researchi.admin.legacy.research.visibility.mapper.ResearchVisibilityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResearchVisibilityService {

    private final ResearchVisibilityMapper researchVisibilityMapper;

    public ResearchVisibilityService(ResearchVisibilityMapper researchVisibilityMapper) {
        this.researchVisibilityMapper = researchVisibilityMapper;
    }

    public boolean isHidden(Long researchNo) {
        if (researchNo == null) {
            return false;
        }
        var visibility = researchVisibilityMapper.findByResearchNo(researchNo);
        return visibility != null && "Y".equalsIgnoreCase(visibility.getHiddenYn());
    }

    public List<Long> getHiddenResearchNos() {
        return researchVisibilityMapper.findHiddenResearchNos();
    }

    @Transactional("adminTransactionManager")
    public void hide(Long researchNo, Long hiddenBy) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        researchVisibilityMapper.hide(researchNo, hiddenBy);
    }

    @Transactional("adminTransactionManager")
    public void restore(Long researchNo, Long restoredBy) {
        if (researchNo == null) {
            throw new IllegalArgumentException("researchNo is required.");
        }
        researchVisibilityMapper.restore(researchNo, restoredBy);
    }
}
