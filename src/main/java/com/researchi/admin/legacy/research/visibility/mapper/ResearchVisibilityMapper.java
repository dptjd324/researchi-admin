package com.researchi.admin.legacy.research.visibility.mapper;

import com.researchi.admin.legacy.research.domain.ResearchVisibility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResearchVisibilityMapper {

    ResearchVisibility findByResearchNo(@Param("researchNo") Long researchNo);

    List<Long> findHiddenResearchNos();

    void hide(@Param("researchNo") Long researchNo, @Param("hiddenBy") Long hiddenBy);

    void restore(@Param("researchNo") Long researchNo, @Param("restoredBy") Long restoredBy);
}
