package com.researchi.admin.matching.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminKeywordMatchTarget {

    private Long id;
    private Long matchJobId;
    private Long applicationId;
    private String matchedKeyword;
    private Integer matchScore;
    private String notifyEmailYn;
    private String notifySmsYn;
    private String notifyStatus;
    private LocalDateTime sentAt;
    private String failReason;
    private LocalDateTime createdAt;

}
