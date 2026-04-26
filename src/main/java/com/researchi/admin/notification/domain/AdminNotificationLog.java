package com.researchi.admin.notification.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminNotificationLog {

    private Long id;
    private Long applicationId;
    private Long documentSrl;
    private String channelType;
    private String targetAddressMasked;
    private String keywordSummary;
    private String sendStatus;
    private String failReason;
    private LocalDateTime createdAt;

}
