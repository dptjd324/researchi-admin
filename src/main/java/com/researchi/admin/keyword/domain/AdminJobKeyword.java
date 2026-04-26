package com.researchi.admin.keyword.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminJobKeyword {

    private Long id;
    private Long documentSrl;
    private String keyword;
    private String keywordNormalized;
    private String sourceType;

}
