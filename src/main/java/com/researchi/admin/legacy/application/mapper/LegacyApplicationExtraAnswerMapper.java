package com.researchi.admin.legacy.application.mapper;

import com.researchi.admin.legacy.application.domain.LegacyApplicationExtraAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyApplicationExtraAnswerMapper {

    void insert(LegacyApplicationExtraAnswer answer);

    List<LegacyApplicationExtraAnswer> findByResearchApplication(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq
    );

    List<LegacyApplicationExtraAnswer> findByResearchNo(@Param("researchNo") Long researchNo);

    List<Long> findResearchAppSeqsByAnswerTerms(
            @Param("terms") List<String> terms,
            @Param("limit") int limit
    );
}
