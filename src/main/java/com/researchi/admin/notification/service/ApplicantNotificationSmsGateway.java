package com.researchi.admin.notification.service;

import com.researchi.admin.notification.domain.NotificationSmsRequest;

public interface ApplicantNotificationSmsGateway {

    void dispatch(NotificationSmsRequest request) throws Exception;
}
