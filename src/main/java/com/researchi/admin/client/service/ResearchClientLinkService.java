package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.AdminResearchClientLink;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.mapper.AdminResearchClientLinkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResearchClientLinkService {

    private final AdminResearchClientLinkMapper researchClientLinkMapper;
    private final ClientService clientService;

    public ResearchClientLinkService(
            AdminResearchClientLinkMapper researchClientLinkMapper,
            ClientService clientService
    ) {
        this.researchClientLinkMapper = researchClientLinkMapper;
        this.clientService = clientService;
    }

    public Long getClientId(Long researchNo) {
        AdminResearchClientLink link = researchClientLinkMapper.findByResearchNo(researchNo);
        return link == null ? null : link.getClientId();
    }

    public List<AdminResearchClientLink> getLinksByClientId(Long clientId) {
        return researchClientLinkMapper.findByClientId(clientId);
    }

    @Transactional("adminTransactionManager")
    public void saveLink(Long researchNo, Long clientId) {
        if (researchNo == null || clientId == null) {
            return;
        }
        ClientSummary client = clientService.getClientSummary(clientId);
        AdminResearchClientLink link = new AdminResearchClientLink();
        link.setResearchNo(researchNo);
        link.setClientId(client.id());
        link.setClientName(client.clientName());
        researchClientLinkMapper.upsert(link);
    }

}
