package com.researchi.admin.mailing.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminMailSendTarget {

    private Long id;
    private Long sendJobId;
    private Long applicationId;
    private String targetEmailMasked;
    private String targetName;
    private String sendResult;
    private String failReason;
    private LocalDateTime sentAt;

}
