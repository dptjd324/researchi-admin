package com.researchi.admin.client.service;

import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientImpactServiceTest {

    @Mock
    private AdminJobMetaMapper adminJobMetaMapper;
    @Mock
    private JobService jobService;

    @InjectMocks
    private ClientImpactService clientImpactService;

    @Test
    void summarizeReturnsLinkedJobsForClient() {
        AdminJobMeta firstMeta = new AdminJobMeta();
        firstMeta.setDocumentSrl(1L);
        firstMeta.setClientId(5L);
        firstMeta.setRecruitStatus("RECRUITING");

        when(adminJobMetaMapper.findByClientId(5L)).thenReturn(List.of(firstMeta));
        when(jobService.getJobsByDocumentSrls(List.of(1L))).thenReturn(List.of(
                new JobListItem(1L, "Job A", "content", "PUBLIC", "", "", firstMeta, "newjob")
        ));

        var impact = clientImpactService.summarize(5L);

        assertThat(impact.linkedJobCount()).isEqualTo(1);
        assertThat(impact.linkedJobs().get(0).jobTitle()).isEqualTo("Job A");
    }
}
