package com.researchi.admin.job.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.BoardConfig;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.domain.JobType;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.web.JobForm;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.domain.XeModule;
import com.researchi.admin.xe.service.XeJobService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final XeJobService xeJobService;
    private final AdminJobMetaMapper adminJobMetaMapper;
    private final AdminActionLogService adminActionLogService;
    private final KeywordExtractionService keywordExtractionService;
    private final ClientService clientService;

    public JobService(
            XeJobService xeJobService,
            AdminJobMetaMapper adminJobMetaMapper,
            AdminActionLogService adminActionLogService,
            KeywordExtractionService keywordExtractionService,
            ClientService clientService
    ) {
        this.xeJobService = xeJobService;
        this.adminJobMetaMapper = adminJobMetaMapper;
        this.adminActionLogService = adminActionLogService;
        this.keywordExtractionService = keywordExtractionService;
        this.clientService = clientService;
    }

    public List<JobListItem> getJobsByDocumentSrls(List<Long> documentSrls) {
        if (documentSrls == null || documentSrls.isEmpty()) {
            return List.of();
        }
        List<Long> distinctDocumentSrls = documentSrls.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctDocumentSrls.isEmpty()) {
            return List.of();
        }
        return toJobListItems(
                xeJobService.getJobDocumentsByIds(distinctDocumentSrls),
                adminJobMetaMapper.findByDocumentSrls(distinctDocumentSrls)
        );
    }

    public List<JobListItem> getRecentJobOptions(Long selectedDocumentSrl, int limit) {
        int safeLimit = Math.max(1, limit);
        List<JobListItem> jobs = getJobPage(null, null, safeLimit, 0);
        if (selectedDocumentSrl == null || jobs.stream().anyMatch(job -> selectedDocumentSrl.equals(job.getDocumentSrl()))) {
            return jobs;
        }

        Map<Long, JobListItem> merged = new LinkedHashMap<>();
        for (JobListItem job : getJobsByDocumentSrls(List.of(selectedDocumentSrl))) {
            merged.put(job.getDocumentSrl(), job);
        }
        for (JobListItem job : jobs) {
            merged.put(job.getDocumentSrl(), job);
        }
        return List.copyOf(merged.values());
    }

    public List<JobListItem> getRecentApplicationJobOptions(Long selectedDocumentSrl, int limit) {
        int safeLimit = Math.max(1, limit);
        List<XeJobDocument> documents = xeJobService.getApplicationJobDocumentsPage(null, null, List.of(), safeLimit, 0);
        if (selectedDocumentSrl != null && documents.stream().noneMatch(document -> selectedDocumentSrl.equals(document.getDocumentSrl()))) {
            XeJobDocument selected = xeJobService.getJobDocument(selectedDocumentSrl);
            if (selected != null && BoardConfig.isApplicationMid(selected.getMid())) {
                documents = new java.util.ArrayList<>(documents);
                documents.add(selected);
            }
        }
        if (documents.isEmpty()) {
            return List.of();
        }
        List<Long> documentSrls = documents.stream().map(XeJobDocument::getDocumentSrl).distinct().toList();
        return toJobListItems(documents, adminJobMetaMapper.findByDocumentSrls(documentSrls));
    }

    public List<Long> findDocumentSrlsByTitle(String keyword) {
        return xeJobService.getJobDocumentSrlsByTitle(normalizedKeyword(keyword), keywordTokens(keyword));
    }

    public List<Long> findApplicationDocumentSrlsByTitle(String keyword) {
        return xeJobService.getApplicationJobDocumentSrlsByTitle(normalizedKeyword(keyword), keywordTokens(keyword));
    }

    public int countJobs(String jobType, String keyword) {
        return xeJobService.countJobDocuments(midForJobType(jobType), normalizedKeyword(keyword), keywordTokens(keyword));
    }

    public List<JobListItem> getJobPage(String jobType, String keyword, int limit, int offset) {
        List<XeJobDocument> documents = xeJobService.getJobDocumentsPage(
                midForJobType(jobType),
                normalizedKeyword(keyword),
                keywordTokens(keyword),
                limit,
                offset
        );
        if (documents.isEmpty()) {
            return List.of();
        }
        List<Long> documentSrls = documents.stream()
                .map(XeJobDocument::getDocumentSrl)
                .toList();
        return toJobListItems(documents, adminJobMetaMapper.findByDocumentSrls(documentSrls));
    }

    public List<JobListItem> getJobPageAfter(String jobType, String keyword, Long afterDocumentSrl, int limit) {
        if (afterDocumentSrl == null) {
            return getJobPage(jobType, keyword, limit, 0);
        }
        List<XeJobDocument> documents = xeJobService.getJobDocumentsAfter(
                midForJobType(jobType),
                normalizedKeyword(keyword),
                keywordTokens(keyword),
                afterDocumentSrl,
                limit
        );
        if (documents.isEmpty()) {
            return List.of();
        }
        List<Long> documentSrls = documents.stream()
                .map(XeJobDocument::getDocumentSrl)
                .toList();
        return toJobListItems(documents, adminJobMetaMapper.findByDocumentSrls(documentSrls));
    }

    private List<JobListItem> toJobListItems(List<XeJobDocument> documents, List<AdminJobMeta> metas) {
        Map<Long, AdminJobMeta> metaByDocumentSrl = new LinkedHashMap<>();
        for (AdminJobMeta meta : metas) {
            if (meta.getDocumentSrl() == null) {
                continue;
            }
            metaByDocumentSrl.put(meta.getDocumentSrl(), meta);
        }

        return documents.stream()
                .map(document -> new JobListItem(
                        document.getDocumentSrl(),
                        document.getTitle(),
                        document.getContent(),
                        document.getStatus(),
                        document.getRegdate(),
                        document.getLastUpdate(),
                        metaByDocumentSrl.get(document.getDocumentSrl()),
                        document.getMid()
                ))
                .sorted(Comparator.comparing(JobListItem::getDocumentSrl).reversed())
                .toList();
    }

    private String midForJobType(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return null;
        }
        return BoardConfig.fromCode(jobType).getMid();
    }

    private String normalizedKeyword(String keyword) {
        String normalized = normalizeSearchText(keyword);
        return normalized.isBlank() ? null : normalized;
    }

    private List<String> keywordTokens(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keyword.trim().split("\\s+"))
                .map(this::normalizeSearchText)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lowered.length());
        for (int i = 0; i < lowered.length(); i++) {
            char ch = lowered.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public JobDetail getJob(Long documentSrl) {
        XeJobDocument document = xeJobService.getJobDocument(documentSrl);
        if (document == null) {
            throw new IllegalArgumentException("공고를 찾을 수 없습니다.");
        }
        AdminJobMeta meta = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        if (meta == null && BoardConfig.isApplicationMid(document.getMid())) {
            meta = createDefaultMetaForExistingDocument(document, fromXeStatus(document.getStatus()));
        }
        hydrateClientSnapshot(meta);
        return new JobDetail(document, meta);
    }

    public boolean isApplicationBoard(Long documentSrl) {
        XeJobDocument document = requireXeJobDocument(documentSrl);
        return BoardConfig.isApplicationMid(document.getMid());
    }

    public void requireApplicationBoard(Long documentSrl) {
        if (!isApplicationBoard(documentSrl)) {
            throw new IllegalArgumentException("Applicant/application features are available only for application boards.");
        }
    }

    public AdminJobMeta ensureJobMeta(Long documentSrl) {
        AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        if (existing != null) {
            return existing;
        }

        XeJobDocument document = requireXeJobDocument(documentSrl);
        if (!BoardConfig.isApplicationMid(document.getMid())) {
            throw new IllegalArgumentException("Applicant/application features are available only for application boards.");
        }
        return createDefaultMetaForExistingDocument(document, fromXeStatus(document.getStatus()));
    }

    public List<XeModule> getJobModules() {
        return xeJobService.getJobModules();
    }

    @Transactional("adminTransactionManager")
    public Long createJob(
            JobForm form,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        BoardConfig boardConfig = BoardConfig.fromCode(form.getJobType());
        if (!boardConfig.isApplicationEnabled()) {
            throw new IllegalArgumentException("Application settings are available only for application boards.");
        }
        Long documentSrl = xeJobService.createJobDocument(
                boardConfig.getMid(),
                form.getTitle(),
                form.getContent(),
                toXeStatus(form.getRecruitStatus()),
                request.getRemoteAddr()
        );

        try {
            AdminJobMeta meta = toAdminJobMeta(documentSrl, form, null);
            insertAdminJobMetaIfMissing(meta);
            verifyAdminMetaExists(documentSrl);
        } catch (RuntimeException ex) {
            logXeAdminInconsistency("JOB_CREATE_ADMIN_META_SAVE_FAILED", documentSrl, boardConfig.getMid(), ex);
            throw ex;
        }
        syncJobKeywordsBestEffort(documentSrl);

        try {
            adminActionLogService.log(
                principal.getId(),
                "JOB_CREATE",
                "JOB",
                String.valueOf(documentSrl),
                jobAuditDetail("공고 등록", documentSrl, form, boardConfig),
                    request
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to write job action log. actionType={}, documentSrl={}", "JOB_CREATE", documentSrl, ex);
        }
        return documentSrl;
    }

    @Transactional("adminTransactionManager")
    public void updateJob(
            Long documentSrl,
            JobForm form,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        xeJobService.updateJobDocument(
                documentSrl,
                BoardConfig.fromCode(form.getJobType()).getMid(),
                form.getTitle(),
                form.getContent(),
                toXeStatus(form.getRecruitStatus())
        );

        try {
            if (BoardConfig.fromCode(form.getJobType()).isApplicationEnabled()) {
                AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
                AdminJobMeta meta = toAdminJobMeta(documentSrl, form, existing);
                if (existing == null) {
                    insertAdminJobMetaIfMissing(meta);
                } else {
                    adminJobMetaMapper.update(meta);
                }
                verifyAdminMetaExists(documentSrl);
            }
        } catch (RuntimeException ex) {
            logXeAdminInconsistency("JOB_UPDATE_ADMIN_META_SAVE_FAILED", documentSrl, BoardConfig.fromCode(form.getJobType()).getMid(), ex);
            throw ex;
        }
        syncJobKeywordsBestEffort(documentSrl);

        try {
            adminActionLogService.log(
                principal.getId(),
                "JOB_UPDATE",
                "JOB",
                String.valueOf(documentSrl),
                jobAuditDetail("공고 수정", documentSrl, form, BoardConfig.fromCode(form.getJobType())),
                    request
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to write job action log. actionType={}, documentSrl={}", "JOB_UPDATE", documentSrl, ex);
        }
    }

    @Transactional("adminTransactionManager")
    public void updateRecruitStatus(
            Long documentSrl,
            String recruitStatus,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        String normalizedRecruitStatus = Objects.requireNonNull(recruitStatus);
        xeJobService.updateJobStatus(documentSrl, toXeStatus(normalizedRecruitStatus));

        String actionDetail = jobStatusAuditDetail(documentSrl, normalizedRecruitStatus, null);
        try {
            XeJobDocument document = requireXeJobDocument(documentSrl);
            actionDetail = jobStatusAuditDetail(documentSrl, normalizedRecruitStatus, document.getMid());
            AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
            if (existing == null && BoardConfig.isApplicationMid(document.getMid())) {
                insertAdminJobMetaIfMissing(defaultMetaForExistingJob(documentSrl, normalizedRecruitStatus));
            } else if (existing != null) {
                existing.setRecruitStatus(normalizedRecruitStatus);
                adminJobMetaMapper.update(existing);
            }
        } catch (RuntimeException ex) {
            logXeAdminInconsistency("JOB_STATUS_ADMIN_META_SAVE_FAILED", documentSrl, null, ex);
            throw ex;
        }

        try {
            adminActionLogService.log(
                principal.getId(),
                "JOB_STATUS_UPDATE",
                "JOB",
                String.valueOf(documentSrl),
                actionDetail,
                    request
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to write job action log. actionType={}, documentSrl={}", "JOB_STATUS_UPDATE", documentSrl, ex);
        }
    }

    public JobForm toForm(JobDetail jobDetail) {
        JobForm form = new JobForm();
        form.setDocumentSrl(jobDetail.getDocument().getDocumentSrl());
        form.setTitle(jobDetail.getDocument().getTitle());
        form.setContent(jobDetail.getDocument().getContent());
        AdminJobMeta meta = jobDetail.getMeta();
        if (meta == null) {
            form.setJobType(BoardConfig.fromMid(jobDetail.getDocument().getMid()).name());
            form.setRecruitStatus(fromXeStatus(jobDetail.getDocument().getStatus()));
            form.setApplicationEnabled(jobDetail.isApplicationBoard());
            form.setAutoSendEnabled(Boolean.FALSE);
            form.setAutoSendRepeatYn("N");
            return form;
        }

        form.setJobType(meta.getJobType());
        form.setRecruitStatus(meta.getRecruitStatus());
        form.setRewardText(meta.getRewardText());
        form.setPlaceText(meta.getPlaceText());
        form.setAgeMin(meta.getAgeMin());
        form.setAgeMax(meta.getAgeMax());
        form.setGenderCode(meta.getGenderCode());
        form.setRegionText(meta.getRegionText());
        form.setBrandText(meta.getBrandText());
        form.setRecruitLimit(meta.getRecruitLimit());
        form.setClientId(meta.getClientId());
        if (meta.getClientId() != null) {
            ClientSummary client = clientService.getClientSummary(meta.getClientId());
            form.setClientName(client.clientName());
            form.setClientEmail(client.primaryEmail());
            form.setClientEmails(client.activeEmails().stream()
                    .filter(email -> client.primaryEmail() == null || !client.primaryEmail().equals(email))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse(null));
        } else {
            form.setClientName(meta.getClientName());
            form.setClientEmail(meta.getClientEmail());
            form.setClientEmails(meta.getClientEmails());
        }
        form.setCloseDate(meta.getCloseDate());
        form.setInternalMemo(meta.getInternalMemo());
        form.setApplicationEnabled("Y".equals(meta.getApplicationEnabled()));
        form.setApplicationFormNotice(meta.getApplicationFormNotice());
        form.setAutoSendEnabled("Y".equals(meta.getAutoSendEnabled()));
        form.setAutoSendMode(meta.getAutoSendMode());
        form.setAutoSendThreshold(meta.getAutoSendThreshold());
        form.setAutoSendTime(meta.getAutoSendTime());
        form.setAutoSendRepeatYn(meta.getAutoSendRepeatYn());
        form.setAutoSendRepeatUnit(meta.getAutoSendRepeatUnit());
        form.setAutoSendTemplateId(meta.getAutoSendTemplateId());
        form.setAutoSendAttachmentType(meta.getAutoSendAttachmentType());
        return form;
    }

    private AdminJobMeta toAdminJobMeta(Long documentSrl, JobForm form, AdminJobMeta existingMeta) {
        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(documentSrl);
        meta.setJobType(form.getJobType());
        meta.setRewardText(form.getRewardText());
        meta.setPlaceText(form.getPlaceText());
        meta.setAgeMin(form.getAgeMin());
        meta.setAgeMax(form.getAgeMax());
        meta.setGenderCode(form.getGenderCode());
        meta.setRegionText(form.getRegionText());
        meta.setBrandText(form.getBrandText());
        meta.setRecruitLimit(form.getRecruitLimit());
        if (form.getClientId() != null) {
            applyClientSnapshot(meta, form.getClientId());
        } else if (existingMeta != null && existingMeta.getClientId() == null) {
            meta.setClientId(null);
            meta.setClientName(existingMeta.getClientName());
            meta.setClientEmail(existingMeta.getClientEmail());
            meta.setClientEmails(existingMeta.getClientEmails());
        } else {
            applyClientSnapshot(meta, null);
        }
        meta.setCloseDate(form.getCloseDate());
        meta.setInternalMemo(form.getInternalMemo());
        meta.setRecruitStatus(form.getRecruitStatus());
        meta.setApplicationEnabled(booleanToYn(form.getApplicationEnabled()));
        meta.setApplicationFormNotice(form.getApplicationFormNotice());
        meta.setAutoSendEnabled(booleanToYn(form.getAutoSendEnabled()));
        meta.setAutoSendMode(form.getAutoSendMode());
        meta.setAutoSendThreshold(form.getAutoSendThreshold());
        meta.setAutoSendTime(form.getAutoSendTime());
        meta.setAutoSendRepeatYn(defaultString(form.getAutoSendRepeatYn(), "N"));
        meta.setAutoSendRepeatUnit(form.getAutoSendRepeatUnit());
        meta.setAutoSendTemplateId(form.getAutoSendTemplateId());
        meta.setAutoSendAttachmentType(defaultString(form.getAutoSendAttachmentType(), "XLSX"));
        return meta;
    }

    private String jobAuditDetail(String action, Long documentSrl, JobForm form, BoardConfig boardConfig) {
        StringBuilder detail = new StringBuilder(action)
                .append(": documentSrl=").append(documentSrl)
                .append(", mid=").append(boardConfig.getMid())
                .append(", title=").append(auditText(form.getTitle()))
                .append(", recruitStatus=").append(form.getRecruitStatus())
                .append(", applicationEnabled=").append(booleanToYn(form.getApplicationEnabled()))
                .append(", closeDate=").append(form.getCloseDate())
                .append(", reward=").append(auditText(form.getRewardText()))
                .append(", place=").append(auditText(form.getPlaceText()))
                .append(", age=").append(form.getAgeMin()).append("-").append(form.getAgeMax())
                .append(", gender=").append(defaultString(form.getGenderCode(), "-"))
                .append(", region=").append(auditText(form.getRegionText()))
                .append(", brand=").append(auditText(form.getBrandText()))
                .append(", recruitLimit=").append(form.getRecruitLimit())
                .append(", clientId=").append(form.getClientId())
                .append(", autoSendEnabled=").append(booleanToYn(form.getAutoSendEnabled()))
                .append(", autoSendMode=").append(defaultString(form.getAutoSendMode(), "-"))
                .append(", autoSendThreshold=").append(form.getAutoSendThreshold())
                .append(", autoSendAttachmentType=").append(defaultString(form.getAutoSendAttachmentType(), "XLSX"));
        return limitAuditDetail(detail.toString());
    }

    private String jobStatusAuditDetail(Long documentSrl, String recruitStatus, String mid) {
        return limitAuditDetail(
                "공고 상태 변경: documentSrl=" + documentSrl
                        + ", mid=" + defaultString(mid, "-")
                        + ", recruitStatus=" + recruitStatus
                        + ", xeStatus=" + toXeStatus(recruitStatus)
        );
    }

    private String auditText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 80) {
            return normalized.substring(0, 80) + "...";
        }
        return normalized;
    }

    private String limitAuditDetail(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private AdminJobMeta defaultMetaForExistingJob(Long documentSrl, String recruitStatus) {
        XeJobDocument document = xeJobService.getJobDocument(documentSrl);
        if (document == null) {
            throw new IllegalArgumentException("공고 메타 정보를 찾을 수 없습니다.");
        }

        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(documentSrl);
        meta.setJobType(JobType.fromMid(document.getMid()).name());
        meta.setRecruitStatus(recruitStatus);
        meta.setApplicationEnabled("Y");
        meta.setAutoSendEnabled("N");
        meta.setAutoSendRepeatYn("N");
        meta.setAutoSendAttachmentType("XLSX");
        return meta;
    }

    private XeJobDocument requireXeJobDocument(Long documentSrl) {
        XeJobDocument document = xeJobService.getJobDocument(documentSrl);
        if (document == null) {
            throw new IllegalArgumentException("공고 메타 정보를 찾을 수 없습니다.");
        }
        return document;
    }

    private AdminJobMeta createDefaultMetaForExistingDocument(XeJobDocument document, String recruitStatus) {
        AdminJobMeta meta = defaultMetaForExistingJob(document.getDocumentSrl(), recruitStatus);
        insertAdminJobMetaIfMissing(meta);
        verifyAdminMetaExists(document.getDocumentSrl());
        log.warn(
                "XE_ADMIN_INCONSISTENCY_REPAIRED reason=ADMIN_META_MISSING documentSrl={} mid={} recruitStatus={}",
                document.getDocumentSrl(),
                document.getMid(),
                recruitStatus
        );
        return meta;
    }

    private void insertAdminJobMetaIfMissing(AdminJobMeta meta) {
        AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(meta.getDocumentSrl());
        if (existing != null) {
            log.warn("Skipped duplicate admin_job_meta insert. documentSrl={}", meta.getDocumentSrl());
            return;
        }
        adminJobMetaMapper.insert(meta);
    }

    private void verifyAdminMetaExists(Long documentSrl) {
        if (adminJobMetaMapper.findByDocumentSrl(documentSrl) == null) {
            throw new IllegalStateException("Admin DB 공고 메타 저장을 확인하지 못했습니다. documentSrl=" + documentSrl);
        }
    }

    private void logXeAdminInconsistency(String reason, Long documentSrl, String mid, RuntimeException ex) {
        log.error(
                "XE_ADMIN_INCONSISTENCY_DETECTED reason={} documentSrl={} mid={}",
                reason,
                documentSrl,
                mid,
                ex
        );
    }

    private void syncJobKeywordsBestEffort(Long documentSrl) {
        try {
            keywordExtractionService.syncJobKeywords(documentSrl);
        } catch (RuntimeException ex) {
            log.warn("Failed to sync job keywords. documentSrl={}", documentSrl, ex);
        }
    }

    private void applyClientSnapshot(AdminJobMeta meta, Long clientId) {
        meta.setClientId(clientId);
        if (clientId == null) {
            meta.setClientName(null);
            meta.setClientEmail(null);
            meta.setClientEmails(null);
            return;
        }
        ClientSummary client = clientService.getClientSummary(clientId);
        meta.setClientName(client.clientName());
        meta.setClientEmail(client.primaryEmail());
        meta.setClientEmails(client.activeEmails().stream()
                .filter(email -> client.primaryEmail() == null || !client.primaryEmail().equals(email))
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null));
    }

    private void hydrateClientSnapshot(AdminJobMeta meta) {
        if (meta == null || meta.getClientId() == null) {
            return;
        }
        applyClientSnapshot(meta, meta.getClientId());
    }

    private String booleanToYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String toXeStatus(String recruitStatus) {
        return switch (Objects.requireNonNull(recruitStatus)) {
            case "RECRUITING" -> "PUBLIC";
            case "CLOSED" -> "CLOSED";
            case "WAITING" -> "TEMP";
            default -> throw new IllegalArgumentException("지원하지 않는 모집 상태입니다.");
        };
    }

    private String fromXeStatus(String xeStatus) {
        return switch (xeStatus) {
            case "PUBLIC" -> "RECRUITING";
            case "CLOSED" -> "CLOSED";
            default -> "WAITING";
        };
    }
}
