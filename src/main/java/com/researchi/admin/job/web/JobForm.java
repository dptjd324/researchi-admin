package com.researchi.admin.job.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class JobForm {

    private Long documentSrl;

    @NotBlank(message = "공고 유형을 선택해 주세요.")
    private String jobType;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 250, message = "제목은 250자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "본문을 입력해 주세요.")
    private String content;

    @NotBlank(message = "모집 상태를 선택해 주세요.")
    private String recruitStatus;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate closeDate;

    private String internalMemo;

    @NotNull(message = "신청 가능 여부를 선택해 주세요.")
    private Boolean applicationEnabled;

    private String applicationFormNotice;

    @NotNull(message = "자동발송 사용 여부를 선택해 주세요.")
    private Boolean autoSendEnabled;

    private String autoSendMode;
    private Integer autoSendThreshold;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime autoSendTime;

    private String autoSendRepeatYn;
    private String autoSendRepeatUnit;
    private Long autoSendTemplateId;
    private String autoSendAttachmentType = "XLSX";

}
