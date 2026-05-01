package com.researchi.admin.job.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminJobMeta {

    private Long id;
    private Long documentSrl;
    private String jobType;
    private String rewardText;
    private String placeText;
    private Integer ageMin;
    private Integer ageMax;
    private String genderCode;
    private String regionText;
    private String brandText;
    private Integer recruitLimit;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private String clientEmails;
    private LocalDate closeDate;
    private String internalMemo;
    private String recruitStatus;
    private String applicationEnabled;
    private String applicationFormNotice;
    private String autoSendEnabled;
    private String autoSendMode;
    private Integer autoSendThreshold;
    private LocalTime autoSendTime;
    private String autoSendRepeatYn;
    private String autoSendRepeatUnit;
    private Long autoSendTemplateId;
    private String autoSendAttachmentType;
    private LocalDateTime lastAutoSentAt;
    private LocalDateTime nextAutoSendAt;
    private String deletedYn;
    private String deleteReason;
    private LocalDateTime deletedAt;
    private LocalDateTime permanentDeleteAfter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
