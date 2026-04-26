package com.researchi.admin.form.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FormFieldForm {

    private Long id;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "[a-z0-9_]+", message = "Use lowercase letters, numbers, or underscores only.")
    private String fieldKey;

    @NotBlank
    @Size(max = 100)
    private String fieldLabel;

    @NotBlank
    private String fieldType;

    @NotNull
    @Min(1)
    @Max(999)
    private Integer fieldOrder;

    private Boolean required = Boolean.FALSE;

    @Size(max = 255)
    private String placeholderText;

    @Size(max = 1000)
    private String helpText;

    @Size(max = 4000)
    private String optionsText;

    private Boolean active = Boolean.TRUE;

}
