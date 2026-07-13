package com.researchi.admin.legacy.research.mapper;

import com.researchi.admin.legacy.research.domain.ResearchMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResearchMasterMapper {

    ResearchMaster findByResearchNo(@Param("researchNo") Long researchNo);

    List<ResearchMaster> findByResearchNos(@Param("researchNos") List<Long> researchNos);

    List<ResearchMaster> findByCompanyName(@Param("companyName") String companyName);

    List<ResearchMaster> findPage(
            @Param("keyword") String keyword,
            @Param("registStart") String registStart,
            @Param("registEnd") String registEnd,
            @Param("title") String title,
            @Param("companyName") String companyName,
            @Param("serverName") String serverName,
            @Param("hiddenResearchNos") List<Long> hiddenResearchNos,
            @Param("hiddenOnly") boolean hiddenOnly,
            @Param("sort") String sort,
            @Param("sortDirection") String sortDirection,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int count(
            @Param("keyword") String keyword,
            @Param("registStart") String registStart,
            @Param("registEnd") String registEnd,
            @Param("title") String title,
            @Param("companyName") String companyName,
            @Param("serverName") String serverName,
            @Param("hiddenResearchNos") List<Long> hiddenResearchNos,
            @Param("hiddenOnly") boolean hiddenOnly,
            @Param("sort") String sort
    );

    Long findNextResearchNo();

    List<Long> findClosedResearchNosBefore(@Param("closeDate") String closeDate);

    void insert(ResearchMaster researchMaster);

    int update(ResearchMaster researchMaster);
}
