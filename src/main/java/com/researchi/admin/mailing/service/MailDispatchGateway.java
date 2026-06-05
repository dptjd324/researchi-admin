package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailDispatchResult;

public interface MailDispatchGateway {

    MailDispatchResult dispatch(MailDispatchRequest request) throws Exception;
}
