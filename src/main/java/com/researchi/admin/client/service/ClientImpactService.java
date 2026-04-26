package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.ClientImpactJob;
import com.researchi.admin.client.domain.ClientImpactSummary;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientImpactService {

    private final JobService jobService;

    public ClientImpactService(JobService jobService) {
        this.jobService = jobService;
    }

    public ClientImpactSummary summarize(Long clientId) {
        if (clientId == null) {
            return new ClientImpactSummary(null, 0, List.of());
        }
        List<ClientImpactJob> linkedJobs = jobService.getJobs().stream()
                .filter(job -> job.getMeta() != null && clientId.equals(job.getMeta().getClientId()))
                .map(this::toImpactJob)
                .toList();
        return new ClientImpactSummary(clientId, linkedJobs.size(), linkedJobs);
    }

    private ClientImpactJob toImpactJob(JobListItem job) {
        return new ClientImpactJob(
                job.getDocumentSrl(),
                job.getTitle(),
                job.getMeta() != null ? job.getMeta().getRecruitStatus() : null
        );
    }
}
