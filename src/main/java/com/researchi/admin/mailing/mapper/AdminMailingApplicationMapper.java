package com.researchi.admin.mailing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminMailingApplicationMapper {

    void updateDeliveryStatus(
            @Param("applicationId") Long applicationId,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("deliveryJobId") Long deliveryJobId,
            @Param("deliveredAt") LocalDateTime deliveredAt
    );
}
