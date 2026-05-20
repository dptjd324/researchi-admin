package com.researchi.admin.notification.mapper;

import com.researchi.admin.dashboard.domain.MonthlyMessageCount;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminNotificationLogMapper {

    void insert(AdminNotificationLog log);

    List<AdminNotificationLog> findByResearchNo(@Param("researchNo") Long researchNo);

    List<AdminNotificationLog> findAll();

    List<AdminNotificationLog> findPage(@Param("limit") int limit, @Param("offset") int offset);

    long countAll();

    LocalDateTime findLatestCreatedAt();

    List<MonthlyMessageCount> countSentSmsByMonth(@Param("startDate") java.time.LocalDate startDate);

    int countSuccessfulDuplicate(
            @Param("researchNo") Long researchNo,
            @Param("applicationId") Long applicationId,
            @Param("channelType") String channelType,
            @Param("keywordSummary") String keywordSummary
    );
}
