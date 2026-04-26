package com.researchi.admin.mailing.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminMailSendJob {

    private Long id;
    private Long documentSrl;
    private String sendType;
    private String triggerType;
    private Long templateId;
    private String attachmentType;
    private Integer recipientCount;
    private Integer excludedCount;
    private Integer blacklistExcludedCount;
    private String sendStatus;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private Integer thresholdSnapshot;
    private Integer targetSnapshotCount;
    private String duplicatePreventKey;
    private Long createdBy;
    private LocalDateTime createdAt;
    private String templateName;
    private String jobTitle;

}
