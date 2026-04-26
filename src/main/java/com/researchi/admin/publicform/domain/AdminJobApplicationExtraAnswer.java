package com.researchi.admin.publicform.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminJobApplicationExtraAnswer {

    private Long id;
    private Long applicationId;
    private Integer answerOrder;
    private String questionLabel;
    private String answerText;
    private LocalDateTime createdAt;
}
