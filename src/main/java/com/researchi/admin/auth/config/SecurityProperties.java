package com.researchi.admin.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private int loginFailLimit = 5;
    private long lockMinutes = 15;
    private String sessionTimeout = "30m";

    public int getLoginFailLimit() {
        return loginFailLimit;
    }

    public void setLoginFailLimit(int loginFailLimit) {
        this.loginFailLimit = loginFailLimit;
    }

    public long getLockMinutes() {
        return lockMinutes;
    }

    public void setLockMinutes(long lockMinutes) {
        this.lockMinutes = lockMinutes;
    }

    public String getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
