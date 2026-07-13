package com.researchi.admin.mailing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMailApplicationClaimMapper {

    int insertIgnore(
            @Param("researchNo") Long researchNo,
            @Param("applicationId") Long applicationId,
            @Param("sendJobId") Long sendJobId
    );

    List<Long> findClaimedApplicationIds(@Param("researchNo") Long researchNo);

    int deleteBySendJobId(@Param("sendJobId") Long sendJobId);

    int deleteExpired(@Param("expiryBefore") LocalDateTime expiryBefore);
}
