package com.researchi.admin.form.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.form.domain.AdminFormField;
import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.form.domain.FormFieldOption;
import com.researchi.admin.form.domain.FormFieldType;
import com.researchi.admin.form.mapper.AdminFormFieldMapper;
import com.researchi.admin.form.web.FormFieldForm;
import com.researchi.admin.job.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormFieldService {

    private static final Pattern OPTION_PATTERN = Pattern.compile("\\{\"value\":\"((?:\\\\.|[^\"\\\\])*)\",\"label\":\"((?:\\\\.|[^\"\\\\])*)\"}");

    private final JobService jobService;
    private final AdminFormFieldMapper adminFormFieldMapper;
    private final AdminActionLogService adminActionLogService;

    public FormFieldService(
            JobService jobService,
            AdminFormFieldMapper adminFormFieldMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.jobService = jobService;
        this.adminFormFieldMapper = adminFormFieldMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public List<FormFieldDetail> getFields(Long documentSrl) {
        jobService.getJob(documentSrl);
        return adminFormFieldMapper.findByDocumentSrl(documentSrl).stream()
                .sorted(Comparator.comparing(AdminFormField::getFieldOrder)
                        .thenComparing(AdminFormField::getId))
                .map(this::toDetail)
                .toList();
    }

    public FormFieldForm getForm(Long documentSrl, Long fieldId) {
        jobService.getJob(documentSrl);
        if (fieldId == null) {
            FormFieldForm form = new FormFieldForm();
            form.setFieldType(FormFieldType.TEXT.name());
            form.setFieldOrder(nextFieldOrder(documentSrl));
            form.setRequired(Boolean.FALSE);
            form.setActive(Boolean.TRUE);
            return form;
        }

        AdminFormField field = requireOwnedField(documentSrl, fieldId);
        FormFieldForm form = new FormFieldForm();
        form.setId(field.getId());
        form.setFieldKey(field.getFieldKey());
        form.setFieldLabel(field.getFieldLabel());
        form.setFieldType(field.getFieldType());
        form.setFieldOrder(field.getFieldOrder());
        form.setRequired("Y".equals(field.getRequiredYn()));
        form.setPlaceholderText(field.getPlaceholderText());
        form.setHelpText(field.getHelpText());
        form.setOptionsText(optionsToText(parseOptions(field.getOptionsJson())));
        form.setActive("Y".equals(field.getActiveYn()));
        return form;
    }

    @Transactional("adminTransactionManager")
    public Long saveField(
            Long documentSrl,
            FormFieldForm form,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        jobService.getJob(documentSrl);
        FormFieldType fieldType = resolveType(form.getFieldType());
        List<FormFieldOption> options = normalizeOptions(form.getOptionsText(), fieldType);
        validateFieldKey(documentSrl, form);

        AdminFormField entity = new AdminFormField();
        entity.setId(form.getId());
        entity.setDocumentSrl(documentSrl);
        entity.setFieldKey(normalizeFieldKey(form.getFieldKey()));
        entity.setFieldLabel(form.getFieldLabel().trim());
        entity.setFieldType(fieldType.name());
        entity.setFieldOrder(form.getFieldOrder());
        entity.setRequiredYn(toYn(form.getRequired()));
        entity.setPlaceholderText(normalizeText(form.getPlaceholderText()));
        entity.setHelpText(normalizeText(form.getHelpText()));
        entity.setOptionsJson(writeOptions(options));
        entity.setActiveYn(toYn(form.getActive()));

        if (form.getId() == null) {
            adminFormFieldMapper.insert(entity);
            adminActionLogService.log(
                    principal.getId(),
                    "FORM_FIELD_CREATE",
                    "FORM_FIELD",
                    String.valueOf(entity.getId()),
                    "Created field for job " + documentSrl,
                    request
            );
            return entity.getId();
        }

        requireOwnedField(documentSrl, form.getId());
        adminFormFieldMapper.update(entity);
        adminActionLogService.log(
                principal.getId(),
                "FORM_FIELD_UPDATE",
                "FORM_FIELD",
                String.valueOf(form.getId()),
                "Updated field for job " + documentSrl,
                request
        );
        return form.getId();
    }

    @Transactional("adminTransactionManager")
    public void deleteField(
            Long documentSrl,
            Long fieldId,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        requireOwnedField(documentSrl, fieldId);
        adminFormFieldMapper.deleteById(fieldId);
        adminActionLogService.log(
                principal.getId(),
                "FORM_FIELD_DELETE",
                "FORM_FIELD",
                String.valueOf(fieldId),
                "Deleted field for job " + documentSrl,
                request
        );
    }

    public int nextFieldOrder(Long documentSrl) {
        return adminFormFieldMapper.findByDocumentSrl(documentSrl).stream()
                .map(AdminFormField::getFieldOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void validateFieldKey(Long documentSrl, FormFieldForm form) {
        String fieldKey = normalizeFieldKey(form.getFieldKey());
        AdminFormField existing = adminFormFieldMapper.findByDocumentSrlAndFieldKey(documentSrl, fieldKey);
        if (existing != null && !existing.getId().equals(form.getId())) {
            throw new IllegalArgumentException("이 공고에 이미 같은 필드 키가 있습니다.");
        }
    }

    private FormFieldType resolveType(String fieldType) {
        try {
            return FormFieldType.valueOf(fieldType.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("지원하지 않는 필드 유형입니다.");
        }
    }

    private List<FormFieldOption> normalizeOptions(String optionsText, FormFieldType fieldType) {
        List<FormFieldOption> options = new ArrayList<>();
        if (optionsText != null) {
            for (String rawLine : optionsText.split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String value = line;
                String label = line;
                int separator = line.indexOf('|');
                if (separator >= 0) {
                    value = line.substring(0, separator).trim();
                    label = line.substring(separator + 1).trim();
                }
                if (value.isEmpty() || label.isEmpty()) {
                    throw new IllegalArgumentException("각 옵션에는 값과 라벨이 모두 필요합니다.");
                }
                options.add(new FormFieldOption(value, label));
            }
        }

        if (fieldType.isOptionsRequired() && options.isEmpty()) {
            throw new IllegalArgumentException("선택한 필드 유형에는 옵션이 필요합니다.");
        }
        if (!fieldType.isOptionsRequired()) {
            return List.of();
        }
        return options;
    }

    private String writeOptions(List<FormFieldOption> options) {
        if (options.isEmpty()) {
            return null;
        }
        return options.stream()
                .map(option -> "{\"value\":\"" + escapeJson(option.value()) + "\",\"label\":\"" + escapeJson(option.label()) + "\"}")
                .reduce((left, right) -> left + "," + right)
                .map(joined -> "[" + joined + "]")
                .orElse("[]");
    }

    private List<FormFieldOption> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        Matcher matcher = OPTION_PATTERN.matcher(optionsJson);
        List<FormFieldOption> options = new ArrayList<>();
        while (matcher.find()) {
            options.add(new FormFieldOption(unescapeJson(matcher.group(1)), unescapeJson(matcher.group(2))));
        }
        if (options.isEmpty()) {
            throw new IllegalStateException("필드 옵션을 읽지 못했습니다.");
        }
        return options;
    }

    private FormFieldDetail toDetail(AdminFormField field) {
        return new FormFieldDetail(
                field.getId(),
                field.getFieldKey(),
                field.getFieldLabel(),
                field.getFieldType(),
                field.getFieldOrder(),
                "Y".equals(field.getRequiredYn()),
                field.getPlaceholderText(),
                field.getHelpText(),
                parseOptions(field.getOptionsJson()),
                "Y".equals(field.getActiveYn())
        );
    }

    private AdminFormField requireOwnedField(Long documentSrl, Long fieldId) {
        AdminFormField field = adminFormFieldMapper.findById(fieldId);
        if (field == null || !documentSrl.equals(field.getDocumentSrl())) {
            throw new IllegalArgumentException("이 공고의 필드를 찾을 수 없습니다.");
        }
        return field;
    }

    private String optionsToText(List<FormFieldOption> options) {
        return options.stream()
                .map(option -> option.value().equals(option.label())
                        ? option.value()
                        : option.value() + "|" + option.label())
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String normalizeFieldKey(String fieldKey) {
        String normalized = fieldKey == null ? "" : fieldKey.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("필드 키는 영문 소문자, 숫자, 밑줄만 사용할 수 있습니다.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
