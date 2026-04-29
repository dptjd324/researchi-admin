package com.researchi.admin.mailing.mapper;

import com.researchi.admin.mailing.domain.AdminMailSendJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMailSendJobMapper {

    void insert(AdminMailSendJob sendJob);

    void updateStatus(AdminMailSendJob sendJob);

    AdminMailSendJob findByDuplicatePreventKey(@Param("duplicatePreventKey") String duplicatePreventKey);

    AdminMailSendJob findById(@Param("id") Long id);

    List<AdminMailSendJob> findAll();

    List<AdminMailSendJob> findPage(@Param("limit") int limit, @Param("offset") int offset);

    long countAll();

    LocalDateTime findLatestActivityAt();

    LocalDateTime findLastSuccessfulThresholdSentAt(@Param("documentSrl") Long documentSrl);

    List<AdminMailSendJob> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<AdminMailSendJob> findDueScheduled(@Param("scheduledAt") LocalDateTime scheduledAt);

    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("sendStatus") String sendStatus,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("currentStatus") String currentStatus
    );
}
