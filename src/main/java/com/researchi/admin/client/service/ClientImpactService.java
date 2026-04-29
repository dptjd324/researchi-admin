package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.ClientImpactJob;
import com.researchi.admin.client.domain.ClientImpactSummary;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientImpactService {

    private final AdminJobMetaMapper adminJobMetaMapper;
    private final JobService jobService;

    public ClientImpactService(AdminJobMetaMapper adminJobMetaMapper, JobService jobService) {
        this.adminJobMetaMapper = adminJobMetaMapper;
        this.jobService = jobService;
    }

    public ClientImpactSummary summarize(Long clientId) {
        if (clientId == null) {
            return new ClientImpactSummary(null, 0, List.of());
        }
        List<AdminJobMeta> metas = adminJobMetaMapper.findByClientId(clientId);
        Map<Long, JobListItem> jobsByDocumentSrl = jobService.getJobsByDocumentSrls(
                        metas.stream().map(AdminJobMeta::getDocumentSrl).toList()
                ).stream()
                .collect(LinkedHashMap::new, (map, job) -> map.put(job.getDocumentSrl(), job), Map::putAll);
        List<ClientImpactJob> linkedJobs = metas.stream()
                .map(meta -> toImpactJob(meta, jobsByDocumentSrl.get(meta.getDocumentSrl())))
                .toList();
        return new ClientImpactSummary(clientId, linkedJobs.size(), linkedJobs);
    }

    private ClientImpactJob toImpactJob(AdminJobMeta meta, JobListItem job) {
        return new ClientImpactJob(
                meta.getDocumentSrl(),
                job == null ? "Job #" + meta.getDocumentSrl() : job.getTitle(),
                meta.getRecruitStatus()
        );
    }
}
