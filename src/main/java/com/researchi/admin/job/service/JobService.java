package com.researchi.admin.job.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.AdminJobMeta;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class JobService {

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

    public List<JobListItem> getJobs() {
        Map<Long, AdminJobMeta> metaByDocumentSrl = new LinkedHashMap<>();
        for (AdminJobMeta meta : adminJobMetaMapper.findAll()) {
            if (meta.getDocumentSrl() == null) {
                continue;
            }
            metaByDocumentSrl.put(meta.getDocumentSrl(), meta);
        }

        return xeJobService.getJobDocuments().stream()
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

    public JobDetail getJob(Long documentSrl) {
        XeJobDocument document = xeJobService.getJobDocument(documentSrl);
        if (document == null) {
            throw new IllegalArgumentException("공고를 찾을 수 없습니다.");
        }
        AdminJobMeta meta = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        hydrateClientSnapshot(meta);
        return new JobDetail(document, meta);
    }

    public AdminJobMeta ensureJobMeta(Long documentSrl) {
        AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        if (existing != null) {
            return existing;
        }

        AdminJobMeta defaultMeta = defaultMetaForExistingJob(documentSrl, fromXeStatus(getJob(documentSrl).getDocument().getStatus()));
        adminJobMetaMapper.insert(defaultMeta);
        return defaultMeta;
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
        JobType jobType = JobType.valueOf(form.getJobType());
        Long documentSrl = xeJobService.createJobDocument(
                jobType.getMid(),
                form.getTitle(),
                form.getContent(),
                toXeStatus(form.getRecruitStatus()),
                request.getRemoteAddr()
        );

        AdminJobMeta meta = toAdminJobMeta(documentSrl, form, null);
        adminJobMetaMapper.insert(meta);
        keywordExtractionService.syncJobKeywords(documentSrl);

        adminActionLogService.log(
                principal.getId(),
                "JOB_CREATE",
                "JOB",
                String.valueOf(documentSrl),
                "공고 등록",
                request
        );
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
                JobType.valueOf(form.getJobType()).getMid(),
                form.getTitle(),
                form.getContent(),
                toXeStatus(form.getRecruitStatus())
        );

        AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        AdminJobMeta meta = toAdminJobMeta(documentSrl, form, existing);
        if (existing == null) {
            adminJobMetaMapper.insert(meta);
        } else {
            adminJobMetaMapper.update(meta);
        }
        keywordExtractionService.syncJobKeywords(documentSrl);

        adminActionLogService.log(
                principal.getId(),
                "JOB_UPDATE",
                "JOB",
                String.valueOf(documentSrl),
                "공고 수정",
                request
        );
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

        AdminJobMeta existing = adminJobMetaMapper.findByDocumentSrl(documentSrl);
        if (existing == null) {
            adminJobMetaMapper.insert(defaultMetaForExistingJob(documentSrl, normalizedRecruitStatus));
        } else {
            existing.setRecruitStatus(normalizedRecruitStatus);
            adminJobMetaMapper.update(existing);
        }

        adminActionLogService.log(
                principal.getId(),
                "JOB_STATUS_UPDATE",
                "JOB",
                String.valueOf(documentSrl),
                "공고 상태 변경: " + normalizedRecruitStatus,
                request
        );
    }

    public JobForm toForm(JobDetail jobDetail) {
        JobForm form = new JobForm();
        form.setDocumentSrl(jobDetail.getDocument().getDocumentSrl());
        form.setTitle(jobDetail.getDocument().getTitle());
        form.setContent(jobDetail.getDocument().getContent());
        AdminJobMeta meta = jobDetail.getMeta();
        if (meta == null) {
            form.setJobType(JobType.fromMid(jobDetail.getDocument().getMid()).name());
            form.setRecruitStatus(fromXeStatus(jobDetail.getDocument().getStatus()));
            form.setApplicationEnabled(Boolean.TRUE);
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
