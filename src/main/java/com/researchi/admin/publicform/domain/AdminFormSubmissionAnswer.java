package com.researchi.admin.publicform.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminFormSubmissionAnswer {

    private Long id;
    private Long applicationId;
    private Long fieldId;
    private String answerText;
    private String answerJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
