package com.researchi.admin.publicform.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminApplicationDuplicateLog {

    private Long id;
    private Long documentSrl;
    private String applicantName;
    private String genderCode;
    private LocalDate birthDate;
    private String mobilePhoneHash;
    private String duplicateFound;
    private Long matchedApplicationId;
    private LocalDateTime checkedAt;

}
