package com.researchi.admin.legacy.matching.domain;

import com.researchi.admin.legacy.research.domain.ResearchApplication;

import java.util.List;

public record LegacyMatchingResult(
        int rowNo,
        ResearchApplication application,
        int matchScore,
        List<String> matchedKeywords,
        List<String> excludedKeywords,
        boolean smsAllowed,
        boolean emailAllowed,
        boolean smsSent,
        boolean emailSent
) {

    public LegacyMatchingResult(
            int rowNo,
            ResearchApplication application,
            int matchScore,
            List<String> matchedKeywords,
            List<String> excludedKeywords
    ) {
        this(rowNo, application, matchScore, matchedKeywords, excludedKeywords, false, false, false, false);
    }

    public LegacyMatchingResult withNotificationStatus(boolean smsSent, boolean emailSent) {
        return new LegacyMatchingResult(
                rowNo, application, matchScore, matchedKeywords, excludedKeywords,
                smsAllowed, emailAllowed, smsSent, emailSent
        );
    }

    public LegacyMatchingResult withConsentStatus(boolean smsAllowed, boolean emailAllowed) {
        return new LegacyMatchingResult(
                rowNo, application, matchScore, matchedKeywords, excludedKeywords,
                smsAllowed, emailAllowed, smsSent, emailSent
        );
    }

    public String matchedKeywordText() {
        return String.join(", ", matchedKeywords);
    }

    public String excludedKeywordText() {
        return String.join(", ", excludedKeywords);
    }

    public boolean hasExcludedKeywords() {
        return !excludedKeywords.isEmpty();
    }
}
