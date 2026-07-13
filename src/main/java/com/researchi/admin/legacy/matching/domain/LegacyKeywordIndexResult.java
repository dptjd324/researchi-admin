package com.researchi.admin.legacy.matching.domain;

public record LegacyKeywordIndexResult(
        int cycleNo,
        int indexedApplicationCount,
        int insertedKeywordCount,
        boolean limitReached,
        int skippedAlreadyIndexedCount,
        boolean alreadyRunning
) {
    public LegacyKeywordIndexResult(int indexedApplicationCount, int insertedKeywordCount) {
        this(0, indexedApplicationCount, insertedKeywordCount, false, 0, false);
    }

    public LegacyKeywordIndexResult(int indexedApplicationCount, int insertedKeywordCount, boolean limitReached) {
        this(0, indexedApplicationCount, insertedKeywordCount, limitReached, 0, false);
    }

    public static LegacyKeywordIndexResult alreadyRunning(int cycleNo) {
        return new LegacyKeywordIndexResult(cycleNo, 0, 0, false, 0, true);
    }

    public LegacyKeywordIndexResult withCycleNo(int cycleNo) {
        return new LegacyKeywordIndexResult(
                cycleNo,
                indexedApplicationCount,
                insertedKeywordCount,
                limitReached,
                skippedAlreadyIndexedCount,
                alreadyRunning
        );
    }
}
