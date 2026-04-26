package com.researchi.admin.publicform.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminPrivacyConsent {

    private Long id;
    private Long applicationId;
    private String consentType;
    private String consentVersion;
    private String agreedYn;
    private String ipAddress;
    private LocalDateTime agreedAt;

}
