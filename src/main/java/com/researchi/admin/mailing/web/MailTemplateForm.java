package com.researchi.admin.mailing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MailTemplateForm {

    private Long id;

    @NotBlank(message = "템플릿 이름을 입력해 주세요.")
    @Size(max = 120, message = "템플릿 이름은 120자 이하로 입력해 주세요.")
    private String templateName;

    @NotBlank(message = "메일 제목을 입력해 주세요.")
    @Size(max = 255, message = "메일 제목은 255자 이하로 입력해 주세요.")
    private String mailSubject;

    @NotBlank(message = "메일 본문을 입력해 주세요.")
    private String mailBody;

    @NotNull(message = "사용 여부를 선택해 주세요.")
    private Boolean active = Boolean.TRUE;

}
