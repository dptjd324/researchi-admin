package com.researchi.admin.notification.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationApplicationRecipient {

    private Long applicationId;
    private Long sourceDocumentSrl;
    private String applicantName;
    private String emailAddressEnc;
    private String emailAddressMasked;
    private String mobilePhoneEnc;
    private String mobilePhoneMasked;

}
