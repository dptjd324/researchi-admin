package com.researchi.admin.blacklist.web;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BlacklistForm {

    private Long id;
    private String blackName;
    private String mobilePhone;
    private LocalDate blackBirthDate;
    private String blackReason;
    private String blackMode;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiresAt;

}
