package com.researchi.admin.legacy.revision.domain;

public class LegacyRevisionLog {

    private Long id;
    private String legacyTableName;
    private String legacyKey;
    private String beforeJson;
    private String actionType;
    private Long changedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLegacyTableName() {
        return legacyTableName;
    }

    public void setLegacyTableName(String legacyTableName) {
        this.legacyTableName = legacyTableName;
    }

    public String getLegacyKey() {
        return legacyKey;
    }

    public void setLegacyKey(String legacyKey) {
        this.legacyKey = legacyKey;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }
}
