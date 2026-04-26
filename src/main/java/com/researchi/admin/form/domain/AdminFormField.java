package com.researchi.admin.form.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminFormField {

    private Long id;
    private Long documentSrl;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Integer fieldOrder;
    private String requiredYn;
    private String placeholderText;
    private String helpText;
    private String optionsJson;
    private String activeYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
