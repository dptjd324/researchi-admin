package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.domain.MailDispatchRequest;

public interface MailDispatchGateway {

    void dispatch(MailDispatchRequest request) throws Exception;
}
