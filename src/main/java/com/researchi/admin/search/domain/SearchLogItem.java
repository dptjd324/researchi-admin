package com.researchi.admin.search.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchLogItem {

    private Long id;
    private String searchType;
    private String keywordText;
    private String conditionJson;
    private Integer resultCount;
    private LocalDateTime searchedAt;

}
