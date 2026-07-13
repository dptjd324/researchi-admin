package com.researchi.admin.legacy.research.service.history;

import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.domain.MailingHistoryItem;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LegacyResearchMailHistoryService {

    private final ResearchMasterService researchMasterService;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminMailSendTargetMapper adminMailSendTargetMapper;

    public LegacyResearchMailHistoryService(
            ResearchMasterService researchMasterService,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminMailSendTargetMapper adminMailSendTargetMapper
    ) {
        this.researchMasterService = researchMasterService;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminMailSendTargetMapper = adminMailSendTargetMapper;
    }

    public List<MailingHistoryItem> getHistory(Long researchNo) {
        List<AdminMailSendJob> jobs = researchNo == null
                ? List.of()
                : adminMailSendJobMapper.findLegacyByResearchNo(researchNo);
        return buildHistoryItems(jobs);
    }

    public List<AdminMailSendJob> getScheduledJobs(Long researchNo) {
        if (researchNo == null) {
            return List.of();
        }
        return adminMailSendJobMapper.findLegacyScheduledByResearchNo(researchNo);
    }

    public int countProvisionCompletedApplications(Long sendJobId) {
        if (sendJobId == null) {
            return 0;
        }
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null || !isLegacyMailJob(sendJob) || !"SENT".equalsIgnoreCase(sendJob.getSendStatus())) {
            return 0;
        }
        return (int) adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                .filter(target -> "SENT".equalsIgnoreCase(target.getSendResult()))
                .map(AdminMailSendTarget::getApplicationId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private List<MailingHistoryItem> buildHistoryItems(List<AdminMailSendJob> jobs) {
        if (jobs.isEmpty()) {
            return List.of();
        }

        List<AdminMailSendJob> displayJobs = jobs.stream()
                .filter(Objects::nonNull)
                .filter(job -> !"SCHEDULED".equalsIgnoreCase(job.getSendStatus()))
                .sorted(Comparator
                        .comparing(this::historyActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminMailSendJob::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        for (AdminMailSendJob job : displayJobs) {
            if (job.getResearchNo() == null) {
                continue;
            }
            try {
                ResearchMaster researchMaster = researchMasterService.getResearchMaster(job.getResearchNo());
                job.setJobTitle(researchMaster.getResearchTitle());
            } catch (RuntimeException ignored) {
                job.setJobTitle("Research #" + job.getResearchNo());
            }
        }

        Map<Long, List<AdminMailSendTarget>> targetsBySendJobId = new LinkedHashMap<>();
        List<Long> sendJobIds = displayJobs.stream()
                .map(AdminMailSendJob::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!sendJobIds.isEmpty()) {
            for (AdminMailSendTarget target : adminMailSendTargetMapper.findBySendJobIds(sendJobIds)) {
                targetsBySendJobId.computeIfAbsent(target.getSendJobId(), ignored -> new ArrayList<>()).add(target);
            }
        }

        Map<Long, Integer> cumulativeSentCountsBySendJobId = cumulativeSentCountsBySendJobId(displayJobs);
        List<MailingHistoryItem> historyItems = new ArrayList<>();
        for (AdminMailSendJob job : displayJobs) {
            List<AdminMailSendTarget> targets = targetsBySendJobId.getOrDefault(job.getId(), List.of());
            historyItems.add(new MailingHistoryItem(
                    job,
                    targets,
                    MailingHistoryItem.recipientAddressesFromTargets(targets),
                    cumulativeSentCountsBySendJobId.getOrDefault(job.getId(), 0)
            ));
        }
        return historyItems;
    }

    private Map<Long, Integer> cumulativeSentCountsBySendJobId(List<AdminMailSendJob> jobs) {
        Map<String, Integer> countsByHistoryKey = new LinkedHashMap<>();
        Map<Long, Integer> countsBySendJobId = new LinkedHashMap<>();
        List<AdminMailSendJob> chronologicalJobs = jobs.stream()
                .filter(job -> job != null && job.getId() != null)
                .sorted(Comparator
                        .comparing(this::historyActivityAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(AdminMailSendJob::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (AdminMailSendJob job : chronologicalJobs) {
            String historyKey = historyKey(job);
            if (historyKey == null) {
                countsBySendJobId.put(job.getId(), 0);
                continue;
            }
            if ("SENT".equals(job.getSendStatus())) {
                countsByHistoryKey.merge(historyKey, nullToZero(job.getTargetSnapshotCount()), Integer::sum);
            }
            countsBySendJobId.put(job.getId(), countsByHistoryKey.getOrDefault(historyKey, 0));
        }
        return countsBySendJobId;
    }

    private String historyKey(AdminMailSendJob job) {
        if (job == null || job.getResearchNo() == null) {
            return null;
        }
        return "RESEARCH:" + job.getResearchNo();
    }

    private LocalDateTime historyActivityAt(AdminMailSendJob job) {
        if (job.getSentAt() != null) {
            return job.getSentAt();
        }
        if ("SCHEDULED".equalsIgnoreCase(job.getSendStatus()) && job.getScheduledAt() != null) {
            return job.getScheduledAt();
        }
        return job.getCreatedAt() != null ? job.getCreatedAt() : job.getScheduledAt();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isLegacyMailJob(AdminMailSendJob job) {
        return job != null && job.getTriggerType() != null && job.getTriggerType().startsWith("LEGACY_");
    }
}
