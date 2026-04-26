package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.ClientMigrationCandidate;
import com.researchi.admin.client.domain.ClientMigrationPreview;
import com.researchi.admin.client.domain.ClientMigrationResult;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ClientMigrationService {

    private final AdminJobMetaMapper adminJobMetaMapper;
    private final JobService jobService;
    private final ClientService clientService;

    public ClientMigrationService(
            AdminJobMetaMapper adminJobMetaMapper,
            JobService jobService,
            ClientService clientService
    ) {
        this.adminJobMetaMapper = adminJobMetaMapper;
        this.jobService = jobService;
        this.clientService = clientService;
    }

    public ClientMigrationPreview previewLegacyJobMigration() {
        Map<Long, String> titles = jobTitles();
        List<ClientMigrationCandidate> candidates = adminJobMetaMapper.findAll().stream()
                .filter(meta -> meta.getClientId() == null)
                .filter(this::hasLegacyClientValues)
                .map(meta -> new ClientMigrationCandidate(
                        meta.getDocumentSrl(),
                        titles.getOrDefault(meta.getDocumentSrl(), "Job #" + meta.getDocumentSrl()),
                        meta.getClientName(),
                        meta.getClientEmail(),
                        meta.getClientEmails()
                ))
                .toList();
        return new ClientMigrationPreview(candidates.size(), candidates);
    }

    @Transactional("adminTransactionManager")
    public ClientMigrationResult migrateLegacyJobClients() {
        int migratedJobCount = 0;
        Map<String, ClientSummary> cache = new LinkedHashMap<>();
        Set<Long> createdClientIds = new LinkedHashSet<>();
        Set<Long> reusedClientIds = new LinkedHashSet<>();

        for (AdminJobMeta meta : adminJobMetaMapper.findAll()) {
            if (meta.getClientId() != null || !hasLegacyClientValues(meta)) {
                continue;
            }
            String key = cacheKey(meta.getClientName(), meta.getClientEmail());
            ClientSummary client = cache.get(key);
            if (client == null) {
                ClientSummary existingClient = clientService.getAllClientSummaries().stream()
                        .filter(summary -> cacheKey(summary.clientName(), summary.primaryEmail()).equals(key))
                        .findFirst()
                        .orElse(null);
                boolean existedBefore = existingClient != null;
                client = clientService.findOrCreateLegacyClient(meta.getClientName(), meta.getClientEmail(), meta.getClientEmails());
                cache.put(key, client);
                if (existedBefore) {
                    reusedClientIds.add(client.id());
                } else {
                    createdClientIds.add(client.id());
                }
            }
            adminJobMetaMapper.updateClientLink(
                    meta.getDocumentSrl(),
                    client.id(),
                    client.clientName(),
                    client.primaryEmail(),
                    additionalEmails(client)
            );
            migratedJobCount++;
        }

        return new ClientMigrationResult(migratedJobCount, createdClientIds.size(), reusedClientIds.size());
    }

    private String additionalEmails(ClientSummary client) {
        return client.activeEmails().stream()
                .filter(email -> client.primaryEmail() == null || !client.primaryEmail().equalsIgnoreCase(email))
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    private boolean hasLegacyClientValues(AdminJobMeta meta) {
        return trimToNull(meta.getClientName()) != null && trimToNull(meta.getClientEmail()) != null;
    }

    private Map<Long, String> jobTitles() {
        Map<Long, String> titles = new LinkedHashMap<>();
        for (JobListItem job : jobService.getJobs()) {
            titles.put(job.getDocumentSrl(), job.getTitle());
        }
        return titles;
    }

    private String cacheKey(String clientName, String primaryEmail) {
        return trimToNull(clientName).toLowerCase(Locale.ROOT) + "|" + trimToNull(primaryEmail).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
