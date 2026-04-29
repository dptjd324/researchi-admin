package com.researchi.admin.job.mapper;

import com.researchi.admin.job.domain.AdminJobMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminJobMetaMapper {

    List<AdminJobMeta> findAll();

    AdminJobMeta findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<AdminJobMeta> findByDocumentSrls(@Param("documentSrls") List<Long> documentSrls);

    List<AdminJobMeta> findByClientId(@Param("clientId") Long clientId);

    List<AdminJobMeta> findEnabledRecruitingJobs();

    void insert(AdminJobMeta adminJobMeta);

    void update(AdminJobMeta adminJobMeta);

    int updateClientLink(
            @Param("documentSrl") Long documentSrl,
            @Param("clientId") Long clientId,
            @Param("clientName") String clientName,
            @Param("clientEmail") String clientEmail,
            @Param("clientEmails") String clientEmails
    );

    int updateSchedulerState(
            @Param("documentSrl") Long documentSrl,
            @Param("lastAutoSentAt") LocalDateTime lastAutoSentAt,
            @Param("nextAutoSendAt") LocalDateTime nextAutoSendAt
    );
}
