package com.researchi.admin.keyword.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminApplicationKeyword {

    private Long id;
    private Long applicationId;
    private String keyword;
    private String keywordNormalized;
    private String sourceType;

}
