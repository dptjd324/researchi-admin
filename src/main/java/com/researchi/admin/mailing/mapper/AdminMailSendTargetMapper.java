package com.researchi.admin.mailing.mapper;

import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMailSendTargetMapper {

    void insert(AdminMailSendTarget sendTarget);

    List<AdminMailSendTarget> findBySendJobIds(@Param("sendJobIds") List<Long> sendJobIds);

    List<AdminMailSendTarget> findBySendJobId(@Param("sendJobId") Long sendJobId);

    int updateResultBySendJobId(
            @Param("sendJobId") Long sendJobId,
            @Param("sendResult") String sendResult,
            @Param("failReason") String failReason,
            @Param("sentAt") java.time.LocalDateTime sentAt
    );
}
