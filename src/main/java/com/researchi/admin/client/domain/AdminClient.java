package com.researchi.admin.client.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminClient {

    private Long id;
    private String clientName;
    private String departmentName;
    private String replyToEmail;
    private String activeYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
