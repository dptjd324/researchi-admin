package com.researchi.admin.application.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationRecord {

    private Long id;
    private Long applicationNumber;
    private Long documentSrl;
    private String applicantName;
    private String genderCode;
    private LocalDate birthDate;
    private String ageText;
    private String jobText;
    private String organizationText;
    private String mobilePhoneEnc;
    private String mobilePhoneMasked;
    private String mobilePhoneDisplay;
    private String telPhoneEnc;
    private String telPhoneMasked;
    private String telPhoneDisplay;
    private String regionText;
    private String addressEnc;
    private String addressMasked;
    private String addressDisplay;
    private String extraComment;
    private String priorResearchText;
    private String emailAddressEnc;
    private String emailAddressMasked;
    private String emailAddressDisplay;
    private String notifyEmailYn;
    private String notifySmsYn;
    private String notifyKeywordYn;
    private String applicationStatus;
    private String isNewApplicant;
    private String isBlacklisted;
    private String blackModeApplied;
    private String provideYn;
    private String deliveryStatus;
    private LocalDateTime deliveredAt;
    private Long deliveryJobId;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private String jobTitle;
    private String jobType;

}
