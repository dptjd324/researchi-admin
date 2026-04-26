package com.researchi.admin.mailing.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminMailTemplate {

    private Long id;
    private String templateName;
    private String mailSubject;
    private String mailBody;
    private String activeYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "Y".equals(activeYn);
    }
}
