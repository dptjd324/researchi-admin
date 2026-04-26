package com.researchi.admin.mailing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MailScheduleForm {

    @NotNull(message = "공고를 선택해 주세요.")
    private Long documentSrl;

    @NotNull(message = "템플릿을 선택해 주세요.")
    private Long templateId;

    @NotBlank(message = "첨부 형식을 선택해 주세요.")
    private String attachmentType = "XLSX";

    @NotNull(message = "예약 발송 시각을 선택해 주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

}
