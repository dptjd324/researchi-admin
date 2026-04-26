package com.researchi.admin.blacklist.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlacklistEntry {

    private Long id;
    private String blackName;
    private String blackMobilePhoneHash;
    private LocalDate blackBirthDate;
    private String blackReason;
    private String blackMode;
    private String activeYn;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private Long matchCount;

    public boolean hasPhoneRule() {
        return blackMobilePhoneHash != null && !blackMobilePhoneHash.isBlank();
    }
}
