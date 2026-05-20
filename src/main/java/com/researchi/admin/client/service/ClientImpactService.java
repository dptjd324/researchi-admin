package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.AdminResearchClientLink;
import com.researchi.admin.client.domain.ClientImpactJob;
import com.researchi.admin.client.domain.ClientImpactSummary;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchMasterMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClientImpactService {

    private final ResearchMasterMapper researchMasterMapper;
    private final ClientService clientService;
    private final ResearchClientLinkService researchClientLinkService;

    public ClientImpactService(
            ResearchMasterMapper researchMasterMapper,
            ClientService clientService,
            ResearchClientLinkService researchClientLinkService
    ) {
        this.researchMasterMapper = researchMasterMapper;
        this.clientService = clientService;
        this.researchClientLinkService = researchClientLinkService;
    }

    public ClientImpactSummary summarize(Long clientId) {
        if (clientId == null) {
            return new ClientImpactSummary(null, 0, List.of());
        }
        ClientSummary client = clientService.getClientSummary(clientId);
        List<ClientImpactJob> linkedJobs = new ArrayList<>();
        LinkedHashSet<Long> linkedResearchNos = new LinkedHashSet<>();
        List<AdminResearchClientLink> researchLinks = researchClientLinkService.getLinksByClientId(clientId);
        Map<Long, ResearchMaster> researchByNo = researchLinks.isEmpty()
                ? Map.of()
                : researchMasterMapper.findByResearchNos(researchLinks.stream().map(AdminResearchClientLink::getResearchNo).toList()).stream()
                .collect(Collectors.toMap(ResearchMaster::getResearchNo, Function.identity(), (left, right) -> left));
        for (AdminResearchClientLink link : researchLinks) {
            linkedResearchNos.add(link.getResearchNo());
            linkedJobs.add(toImpactJob(link, researchByNo.get(link.getResearchNo())));
        }
        for (ResearchMaster research : researchMasterMapper.findByCompanyName(client.clientName())) {
            if (linkedResearchNos.add(research.getResearchNo())) {
                linkedJobs.add(toImpactJob(research));
            }
        }
        return new ClientImpactSummary(clientId, linkedJobs.size(), linkedJobs);
    }

    private ClientImpactJob toImpactJob(ResearchMaster research) {
        return new ClientImpactJob(
                research.getResearchNo(),
                research.getResearchTitle(),
                research.getCloseDateLabel()
        );
    }

    private ClientImpactJob toImpactJob(AdminResearchClientLink link, ResearchMaster research) {
        return new ClientImpactJob(
                link.getResearchNo(),
                research == null ? "좌담회/설문 #" + link.getResearchNo() : research.getResearchTitle(),
                research == null ? "" : research.getCloseDateLabel()
        );
    }
}
