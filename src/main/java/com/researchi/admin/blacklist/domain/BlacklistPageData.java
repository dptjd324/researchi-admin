package com.researchi.admin.blacklist.domain;

import java.util.List;

public record BlacklistPageData(
        List<BlacklistEntry> entries,
        List<BlacklistMatchLogItem> matchLogs,
        List<BlacklistActionLogItem> actionLogs
) {
}
