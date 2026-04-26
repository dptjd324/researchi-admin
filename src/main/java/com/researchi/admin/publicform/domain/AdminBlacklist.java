package com.researchi.admin.publicform.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminBlacklist {

    private Long id;
    private String blackName;
    private String blackMobilePhoneHash;
    private LocalDate blackBirthDate;
    private String blackReason;
    private String blackMode;
    private String activeYn;
    private LocalDateTime expiresAt;
    private String matchType;

}
