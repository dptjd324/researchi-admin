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

    private String primaryPhone;

    private String primaryContactNo;

    @NotBlank(message = "대표 이메일을 입력하세요.")
    private String primaryEmail;

    private Boolean active = Boolean.TRUE;

    private Boolean confirmImpact = Boolean.FALSE;
}
