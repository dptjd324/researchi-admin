package com.researchi.admin.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface OperationsCleanupMapper {

    int deleteDuplicateLogsBefore(@Param("cutoff") LocalDateTime cutoff);

    int deleteBlacklistMatchLogsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deletePrivacyConsentsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteFormAnswersForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteApplicationKeywordsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteNotificationLogsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteMailTargetsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteKeywordMatchTargetsForExpiredApplications(@Param("cutoff") LocalDateTime cutoff);

    int deleteApplicationsBefore(@Param("cutoff") LocalDateTime cutoff);
}
