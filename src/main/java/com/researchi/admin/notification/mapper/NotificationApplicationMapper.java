package com.researchi.admin.notification.mapper;

import com.researchi.admin.notification.domain.NotificationApplicationRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationApplicationMapper {

    NotificationApplicationRecipient findRecipientByApplicationId(@Param("applicationId") Long applicationId);
}
