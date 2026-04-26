package com.researchi.admin.client.web;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientForm {

    private Long id;

    @NotBlank(message = "거래처명을 입력하세요.")
    private String clientName;

    private String departmentName;

    private String primaryContactName;

    @NotBlank(message = "대표 이메일을 입력하세요.")
    private String primaryEmail;

    private String replyToEmail;

    private String additionalEmails;

    private Boolean active = Boolean.TRUE;

    private Boolean confirmImpact = Boolean.FALSE;
}
