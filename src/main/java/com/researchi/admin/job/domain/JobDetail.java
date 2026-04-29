package com.researchi.admin.job.domain;

import com.researchi.admin.xe.domain.XeJobDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobDetail {

    private XeJobDocument document;
    private AdminJobMeta meta;

    public String getJobType() {
        if (meta != null && meta.getJobType() != null) {
            return meta.getJobType();
        }
        return BoardConfig.fromMid(document.getMid()).name();
    }

    public boolean isApplicationBoard() {
        return BoardConfig.isApplicationMid(document.getMid());
    }
}
