package com.researchi.admin.job.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobListItem {

    private final Long documentSrl;
    private final String title;
    private final String content;
    private final String xeStatus;
    private final String regdate;
    private final String lastUpdate;
    private final AdminJobMeta meta;
    private final String mid;

    public String getJobType() {
        if (meta != null && meta.getJobType() != null) {
            return meta.getJobType();
        }
        return BoardConfig.fromMid(mid).name();
    }

    public boolean isApplicationBoard() {
        return BoardConfig.isApplicationMid(mid);
    }
}
