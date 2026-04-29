package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.ClientSummary;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientMigrationServiceTest {

    @Mock
    private AdminJobMetaMapper adminJobMetaMapper;
    @Mock
    private JobService jobService;
    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientMigrationService clientMigrationService;

    @Test
    void previewReturnsOnlyLegacyUnlinkedCandidates() {
        AdminJobMeta legacy = new AdminJobMeta();
        legacy.setDocumentSrl(9L);
        legacy.setClientName("Client A");
        legacy.setClientEmail("owner@example.com");

        AdminJobMeta alreadyLinked = new AdminJobMeta();
        alreadyLinked.setDocumentSrl(10L);
        alreadyLinked.setClientId(5L);
        alreadyLinked.setClientName("Linked");
        alreadyLinked.setClientEmail("linked@example.com");

        when(adminJobMetaMapper.findAll()).thenReturn(List.of(legacy, alreadyLinked));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(
                new JobListItem(9L, "Survey Job", "content", "PUBLIC", "", "", legacy, "newjob")
        ));

        var preview = clientMigrationService.previewLegacyJobMigration();

        assertThat(preview.candidateCount()).isEqualTo(1);
        assertThat(preview.candidates().get(0).documentSrl()).isEqualTo(9L);
    }

    @Test
    void migrateLinksLegacyJobToCreatedClient() {
        AdminJobMeta legacy = new AdminJobMeta();
        legacy.setDocumentSrl(9L);
        legacy.setClientName("Client A");
        legacy.setClientEmail("owner@example.com");
        legacy.setClientEmails("team@example.com");

        when(adminJobMetaMapper.findAll()).thenReturn(List.of(legacy));
        when(clientService.getAllClientSummaries()).thenReturn(List.of());
        when(clientService.findOrCreateLegacyClient("Client A", "owner@example.com", "team@example.com"))
                .thenReturn(new ClientSummary(5L, "Client A", null, null, "owner@example.com", "owner@example.com", List.of("owner@example.com", "team@example.com"), true));

        var result = clientMigrationService.migrateLegacyJobClients();

        assertThat(result.migratedJobCount()).isEqualTo(1);
        assertThat(result.createdClientCount()).isEqualTo(1);
        verify(adminJobMetaMapper).updateClientLink(9L, 5L, "Client A", "owner@example.com", "team@example.com");
    }
}
