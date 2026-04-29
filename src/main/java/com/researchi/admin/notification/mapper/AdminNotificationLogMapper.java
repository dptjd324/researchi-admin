package com.researchi.admin.notification.mapper;

import com.researchi.admin.notification.domain.AdminNotificationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminNotificationLogMapper {

    void insert(AdminNotificationLog log);

    List<AdminNotificationLog> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<AdminNotificationLog> findAll();

    List<AdminNotificationLog> findPage(@Param("limit") int limit, @Param("offset") int offset);

    long countAll();

    LocalDateTime findLatestCreatedAt();

    int countSuccessfulDuplicate(
            @Param("documentSrl") Long documentSrl,
            @Param("applicationId") Long applicationId,
            @Param("channelType") String channelType,
            @Param("keywordSummary") String keywordSummary
    );
}
