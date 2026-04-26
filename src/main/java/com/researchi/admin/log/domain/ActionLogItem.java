package com.researchi.admin.log.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActionLogItem {

    private Long id;
    private Long adminUserId;
    private String loginId;
    private String userName;
    private String actionType;
    private String targetType;
    private String targetId;
    private String actionDetail;
    private String ipAddress;
    private LocalDateTime createdAt;

}
