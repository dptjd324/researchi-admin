package com.researchi.admin.legacy.research.mapper;

import com.researchi.admin.legacy.research.domain.ApplicantResearchHistoryItem;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchApplicationDuplicateGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResearchApplicationMapper {

    ResearchApplication findByResearchNoAndSeq(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq
    );

    List<ResearchApplication> findPageByResearchNo(
            @Param("researchNo") Long researchNo,
            @Param("search") com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm search,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ResearchApplication> findPage(
            @Param("search") com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm search,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ResearchApplication> findAllByResearchNo(@Param("researchNo") Long researchNo);

    List<ResearchApplication> findAll();

    List<ResearchApplication> findAllPage(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ResearchApplication> findMatchingIndexCandidatePage(
            @Param("condition") LegacyMatchingSearchCondition condition,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ResearchApplication> findUnprovidedByResearchNo(@Param("researchNo") Long researchNo);

    List<Long> findBlacklistedUnprovidedSeqsByResearchNo(@Param("researchNo") Long researchNo);

    List<ResearchApplicationDuplicateGroup> findDuplicateGroupsByResearchNo(@Param("researchNo") Long researchNo);

    List<ResearchApplication> findByResearchNoAndSeqs(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeqs") List<Long> researchAppSeqs
    );

    List<ResearchApplication> findUnprovidedByResearchNoAndSeqs(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeqs") List<Long> researchAppSeqs
    );

    List<ResearchApplication> findByResearchNoAndRegistDate(
            @Param("researchNo") Long researchNo,
            @Param("registDate") String registDate
    );

    Long findDuplicateSeqByNameAndPhone(
            @Param("researchNo") Long researchNo,
            @Param("appName") String appName,
            @Param("appHphone") String appHphone
    );

    List<ApplicantResearchHistoryItem> findApplicantResearchHistory(
            @Param("appName") String appName,
            @Param("appBirth") String appBirth,
            @Param("appHphone") String appHphone,
            @Param("appTele") String appTele
    );

    Long findNextResearchAppSeq();

    void insert(ResearchApplication application);

    int incrementCounts(@Param("researchNo") Long researchNo);

    int countByResearchNo(
            @Param("researchNo") Long researchNo,
            @Param("search") com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm search
    );

    int count(
            @Param("search") com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm search
    );

    int countAllByResearchNo(@Param("researchNo") Long researchNo);

    int countUnprovidedByResearchNo(@Param("researchNo") Long researchNo);

    int updateProvideYn(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq,
            @Param("provideYn") String provideYn
    );
}
