package com.researchi.admin.mailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private boolean simulateSend = true;
    private String fromAddress = "no-reply@researchi.local";

    public boolean isSimulateSend() {
        return simulateSend;
    }

    public void setSimulateSend(boolean simulateSend) {
        this.simulateSend = simulateSend;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
