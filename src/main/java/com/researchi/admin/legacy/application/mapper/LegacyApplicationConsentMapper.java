package com.researchi.admin.legacy.application.mapper;

import com.researchi.admin.legacy.application.domain.LegacyApplicationConsent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LegacyApplicationConsentMapper {

    void insert(LegacyApplicationConsent consent);

    LegacyApplicationConsent findByApplication(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq
    );

    List<Long> findActiveFutureRecruitmentApplicationSeqs(
            @Param("researchAppSeqs") List<Long> researchAppSeqs,
            @Param("now") LocalDateTime now
    );
}
