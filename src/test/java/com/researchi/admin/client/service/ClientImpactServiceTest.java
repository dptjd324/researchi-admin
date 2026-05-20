package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.AdminResearchClientLink;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchMasterMapper;
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
    private ResearchMasterMapper researchMasterMapper;
    @Mock
    private ClientService clientService;
    @Mock
    private ResearchClientLinkService researchClientLinkService;

    @InjectMocks
    private ClientImpactService clientImpactService;

    @Test
    void summarizeReturnsResearchLinksAndCompanyNameMatches() {
        AdminResearchClientLink link = new AdminResearchClientLink();
        link.setResearchNo(46431L);
        link.setClientId(5L);

        ResearchMaster linkedResearch = new ResearchMaster();
        linkedResearch.setResearchNo(46431L);
        linkedResearch.setResearchTitle("Linked Research");

        ResearchMaster companyResearch = new ResearchMaster();
        companyResearch.setResearchNo(46432L);
        companyResearch.setResearchTitle("Company Research");
        companyResearch.setCompanyName("Client A");

        when(clientService.getClientSummary(5L)).thenReturn(clientSummary());
        when(researchClientLinkService.getLinksByClientId(5L)).thenReturn(List.of(link));
        when(researchMasterMapper.findByResearchNos(List.of(46431L))).thenReturn(List.of(linkedResearch));
        when(researchMasterMapper.findByCompanyName("Client A")).thenReturn(List.of(companyResearch));

        var impact = clientImpactService.summarize(5L);

        assertThat(impact.linkedJobCount()).isEqualTo(2);
        assertThat(impact.linkedJobs()).extracting("jobTitle")
                .containsExactly("Linked Research", "Company Research");
    }

    @Test
    void summarizeDoesNotDuplicateCompanyNameMatchAlreadyLinkedByResearchNo() {
        AdminResearchClientLink link = new AdminResearchClientLink();
        link.setResearchNo(46431L);
        link.setClientId(5L);

        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46431L);
        research.setResearchTitle("Old Admin Research");
        research.setCompanyName("Client A");

        when(clientService.getClientSummary(5L)).thenReturn(clientSummary());
        when(researchClientLinkService.getLinksByClientId(5L)).thenReturn(List.of(link));
        when(researchMasterMapper.findByResearchNos(List.of(46431L))).thenReturn(List.of(research));
        when(researchMasterMapper.findByCompanyName("Client A")).thenReturn(List.of(research));

        var impact = clientImpactService.summarize(5L);

        assertThat(impact.linkedJobCount()).isEqualTo(1);
        assertThat(impact.linkedJobs()).extracting("jobTitle")
                .containsExactly("Old Admin Research");
    }

    private ClientSummary clientSummary() {
        return new ClientSummary(
                5L,
                "Client A",
                null,
                null,
                null,
                "owner@example.com",
                "owner@example.com",
                List.of("owner@example.com"),
                true
        );
    }
}
