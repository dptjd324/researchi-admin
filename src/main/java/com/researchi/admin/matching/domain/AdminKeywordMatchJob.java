package com.researchi.admin.matching.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminKeywordMatchJob {

    private Long id;
    private Long documentSrl;
    private String matchStatus;
    private Integer matchedCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

}
