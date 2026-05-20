package com.researchi.admin.export.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminExportLog {

    private Long id;
    private Long researchNo;
    private String exportType;
    private String fileName;
    private Integer exportedCount;
    private LocalDateTime exportedAt;

}
