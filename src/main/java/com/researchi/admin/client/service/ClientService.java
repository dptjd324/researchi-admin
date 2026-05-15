package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.AdminClient;
import com.researchi.admin.client.domain.AdminClientContact;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.mapper.AdminClientContactMapper;
import com.researchi.admin.client.mapper.AdminClientMapper;
import com.researchi.admin.client.web.ClientForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {

    private final AdminClientMapper adminClientMapper;
    private final AdminClientContactMapper adminClientContactMapper;

    public ClientService(
            AdminClientMapper adminClientMapper,
            AdminClientContactMapper adminClientContactMapper
    ) {
        this.adminClientMapper = adminClientMapper;
        this.adminClientContactMapper = adminClientContactMapper;
    }

    public List<ClientSummary> getClientSummaries() {
        return summarize(adminClientMapper.findAllActive());
    }

    public List<ClientSummary> getAllClientSummaries() {
        return summarize(adminClientMapper.findAll());
    }

    public ClientSummary getClientSummary(Long clientId) {
        AdminClient client = requiredClient(clientId);
        return toSummary(client, adminClientContactMapper.findByClientId(clientId));
    }

    public ClientForm toForm(Long clientId) {
        ClientSummary summary = getClientSummary(clientId);
        ClientForm form = new ClientForm();
        form.setId(summary.id());
        form.setClientName(summary.clientName());
        form.setDepartmentName(summary.departmentName());
        form.setPrimaryContactName(summary.primaryContactName());
        form.setPrimaryContactNo(summary.primaryContactNo());
        form.setPrimaryEmail(summary.primaryEmail());
        form.setActive(summary.active());
        form.setConfirmImpact(Boolean.FALSE);
        return form;
    }

    public List<String> getActiveRecipientEmails(Long clientId) {
        return getClientSummary(clientId).activeEmails();
    }

    @Transactional("adminTransactionManager")
    public Long save(ClientForm form) {
        AdminClient client = form.getId() == null ? new AdminClient() : requiredClient(form.getId());
        String primaryEmail = normalizeEmail(form.getPrimaryEmail());
        if (primaryEmail == null) {
            throw new IllegalArgumentException("대표 이메일을 입력하세요.");
        }
        client.setClientName(form.getClientName().trim());
        client.setDepartmentName(trimToNull(form.getDepartmentName()));
        client.setReplyToEmail(primaryEmail);
        client.setActiveYn(Boolean.FALSE.equals(form.getActive()) ? "N" : "Y");
        if (form.getId() == null) {
            adminClientMapper.insert(client);
        } else {
            int updatedRows = adminClientMapper.update(client);
            if (updatedRows == 0) {
                throw new IllegalArgumentException("변경되지 않았습니다. 거래처 정보를 다시 확인한 뒤 저장해 주세요.");
            }
        }

        adminClientContactMapper.deleteByClientId(client.getId());
        List<ContactSeed> contacts = normalizeContacts(form);
        for (int index = 0; index < contacts.size(); index++) {
            ContactSeed seed = contacts.get(index);
            AdminClientContact contact = new AdminClientContact();
            contact.setClientId(client.getId());
            contact.setContactName(seed.contactName());
            contact.setEmail(seed.email());
            contact.setContactNo(seed.contactNo());
            contact.setPrimaryYn(index == 0 ? "Y" : "N");
            contact.setActiveYn("Y");
            adminClientContactMapper.insert(contact);
        }
        return client.getId();
    }

    @Transactional("adminTransactionManager")
    public void deleteClient(Long clientId) {
        adminClientContactMapper.deleteByClientId(clientId);
        adminClientMapper.deleteById(clientId);
    }

    @Transactional("adminTransactionManager")
    public ClientSummary findOrCreateLegacyClient(String clientName, String primaryEmail, String additionalEmails) {
        String normalizedName = trimToNull(clientName);
        String normalizedPrimaryEmail = normalizeEmail(primaryEmail);
        if (normalizedName == null || normalizedPrimaryEmail == null) {
            throw new IllegalArgumentException("기존 거래처 마이그레이션에 필요한 이름 또는 대표 이메일이 없습니다.");
        }

        for (ClientSummary summary : getAllClientSummaries()) {
            if (sameValue(summary.clientName(), normalizedName) && sameValue(summary.primaryEmail(), normalizedPrimaryEmail)) {
                return summary;
            }
        }

        ClientForm form = new ClientForm();
        form.setClientName(normalizedName);
        form.setPrimaryEmail(normalizedPrimaryEmail);
        form.setActive(Boolean.TRUE);
        Long clientId = save(form);
        return getClientSummary(clientId);
    }

    private List<ClientSummary> summarize(List<AdminClient> clients) {
        List<ClientSummary> summaries = new ArrayList<>();
        for (AdminClient client : clients) {
            summaries.add(toSummary(client, adminClientContactMapper.findByClientId(client.getId())));
        }
        return summaries;
    }

    private ClientSummary toSummary(AdminClient client, List<AdminClientContact> contacts) {
        List<AdminClientContact> activeContacts = contacts.stream()
                .filter(contact -> !"N".equals(contact.getActiveYn()))
                .sorted((left, right) -> {
                    if (left.getPrimaryYn().equals(right.getPrimaryYn())) {
                        Long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
                        Long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
                        return leftId.compareTo(rightId);
                    }
                    return "Y".equals(left.getPrimaryYn()) ? -1 : 1;
                })
                .toList();
        String primaryEmail = activeContacts.isEmpty() ? null : activeContacts.get(0).getEmail();
        List<String> activeEmails = primaryEmail == null ? List.of() : List.of(primaryEmail);
        String primaryContactName = activeContacts.isEmpty() ? null : activeContacts.get(0).getContactName();
        String primaryContactNo = activeContacts.isEmpty() ? null : activeContacts.get(0).getContactNo();
        String replyToEmail = normalizeEmail(client.getReplyToEmail());
        if (replyToEmail == null) {
            replyToEmail = primaryEmail;
        }
        return new ClientSummary(
                client.getId(),
                client.getClientName(),
                client.getDepartmentName(),
                primaryContactName,
                primaryContactNo,
                primaryEmail,
                replyToEmail,
                activeEmails,
                !"N".equals(client.getActiveYn())
        );
    }

    private AdminClient requiredClient(Long clientId) {
        AdminClient client = adminClientMapper.findById(clientId);
        if (client == null) {
            throw new IllegalArgumentException("거래처를 찾을 수 없습니다.");
        }
        return client;
    }

    private List<ContactSeed> normalizeContacts(ClientForm form) {
        List<ContactSeed> seeds = new ArrayList<>();
        String primaryEmail = normalizeEmail(form.getPrimaryEmail());
        if (primaryEmail == null) {
            throw new IllegalArgumentException("대표 이메일을 입력하세요.");
        }
        seeds.add(new ContactSeed(
                trimToNull(form.getPrimaryContactName()),
                primaryEmail,
                trimToNull(form.getPrimaryContactNo())
        ));
        return seeds;
    }

    private String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        if (trimmed == null || !trimmed.contains("@")) {
            return null;
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean sameValue(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private record ContactSeed(String contactName, String email, String contactNo) {
    }
}
