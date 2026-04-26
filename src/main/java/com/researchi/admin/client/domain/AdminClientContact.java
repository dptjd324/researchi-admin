package com.researchi.admin.client.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminClientContact {

    private Long id;
    private Long clientId;
    private String contactName;
    private String email;
    private String primaryYn;
    private String activeYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
