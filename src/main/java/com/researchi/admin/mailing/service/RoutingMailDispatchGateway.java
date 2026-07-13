package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RoutingMailDispatchGateway implements MailDispatchGateway {

    private final MailProperties mailProperties;
    private final SpringMailDispatchGateway springMailDispatchGateway;
    private final NaverOutboundMailerGateway naverOutboundMailerGateway;

    public RoutingMailDispatchGateway(
            MailProperties mailProperties,
            SpringMailDispatchGateway springMailDispatchGateway,
            NaverOutboundMailerGateway naverOutboundMailerGateway
    ) {
        this.mailProperties = mailProperties;
        this.springMailDispatchGateway = springMailDispatchGateway;
        this.naverOutboundMailerGateway = naverOutboundMailerGateway;
    }

    @Override
    public MailDispatchResult dispatch(MailDispatchRequest request) throws Exception {
        String provider = mailProperties.getProvider() == null ? "smtp" : mailProperties.getProvider().toLowerCase(Locale.ROOT);
        if ("naver-outbound-mailer".equals(provider) || "naver".equals(provider)) {
            return naverOutboundMailerGateway.dispatch(request);
        }
        return springMailDispatchGateway.dispatch(request);
    }
}
