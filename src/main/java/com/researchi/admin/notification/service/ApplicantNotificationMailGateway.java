package com.researchi.admin.notification.service;

import com.researchi.admin.notification.domain.NotificationEmailRequest;

public interface ApplicantNotificationMailGateway {

    void dispatch(NotificationEmailRequest request) throws Exception;
}
