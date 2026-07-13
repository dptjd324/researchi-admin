package com.researchi.admin.legacy.research.service.recipient;

import com.researchi.admin.legacy.blacklist.mapper.LegacyBlacklistMapper;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSnapshot;
import com.researchi.admin.legacy.research.service.schedule.LegacyResearchScheduledTargetSnapshot;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.mapper.AdminMailApplicationClaimMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LegacyResearchRecipientService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ResearchApplicationMapper researchApplicationMapper;
    private final LegacyBlacklistMapper legacyBlacklistMapper;
    private final AdminMailSendTargetMapper adminMailSendTargetMapper;
    private final AdminMailApplicationClaimMapper adminMailApplicationClaimMapper;

    public LegacyResearchRecipientService(
            ResearchApplicationMapper researchApplicationMapper,
            LegacyBlacklistMapper legacyBlacklistMapper,
            AdminMailSendTargetMapper adminMailSendTargetMapper,
            AdminMailApplicationClaimMapper adminMailApplicationClaimMapper
    ) {
        this.researchApplicationMapper = researchApplicationMapper;
        this.legacyBlacklistMapper = legacyBlacklistMapper;
        this.adminMailSendTargetMapper = adminMailSendTargetMapper;
        this.adminMailApplicationClaimMapper = adminMailApplicationClaimMapper;
    }

    public LegacyResearchMailSnapshot loadSnapshot(Long researchNo) {
        Set<Long> claimedIds = claimedApplicationIds(researchNo);
        List<Long> includedApplicationIds = new ArrayList<>();
        int blacklistExcludedCount = 0;
        for (ResearchApplication application : researchApplicationMapper.findAllByResearchNo(researchNo)) {
            if (claimedIds.contains(application.getResearchAppSeq())) {
                continue;
            }
            if (isBlacklisted(application)) {
                blacklistExcludedCount++;
                continue;
            }
            if (!isUnprovided(application)) {
                continue;
            }
            includedApplicationIds.add(application.getResearchAppSeq());
        }
        return new LegacyResearchMailSnapshot(includedApplicationIds, blacklistExcludedCount);
    }

    public LegacyResearchMailSnapshot loadDailyScheduledSnapshot(Long researchNo) {
        String today = LocalDate.now().format(LEGACY_DATE);
        Set<Long> alreadySentIds = new LinkedHashSet<>(
                adminMailSendTargetMapper.findSentApplicationIdsByResearchNoAndTriggerPrefix(researchNo, "LEGACY_")
        );
        Set<Long> claimedIds = claimedApplicationIds(researchNo);
        List<Long> includedApplicationIds = new ArrayList<>();
        int blacklistExcludedCount = 0;
        for (ResearchApplication application : researchApplicationMapper.findByResearchNoAndRegistDate(researchNo, today)) {
            if (claimedIds.contains(application.getResearchAppSeq())) {
                continue;
            }
            if (alreadySentIds.contains(application.getResearchAppSeq())) {
                continue;
            }
            if (isBlacklisted(application)) {
                blacklistExcludedCount++;
                continue;
            }
            if (!isUnprovided(application)) {
                continue;
            }
            includedApplicationIds.add(application.getResearchAppSeq());
        }
        return new LegacyResearchMailSnapshot(includedApplicationIds, blacklistExcludedCount);
    }

    public LegacyResearchScheduledTargetSnapshot loadScheduledTargetSnapshot(AdminMailSendJob sendJob) {
        List<Long> scheduledApplicationIds = scheduledApplicationIds(sendJob.getId());
        Set<Long> scheduledIdSet = new LinkedHashSet<>(scheduledApplicationIds);
        List<ResearchApplication> scheduledApplications = researchApplicationMapper.findAllByResearchNo(sendJob.getResearchNo()).stream()
                .filter(application -> scheduledIdSet.contains(application.getResearchAppSeq()))
                .toList();
        List<Long> blacklistExcludedIds = scheduledApplications.stream()
                .filter(this::isBlacklisted)
                .map(ResearchApplication::getResearchAppSeq)
                .distinct()
                .toList();
        Set<Long> blacklistExcludedSet = new LinkedHashSet<>(blacklistExcludedIds);
        List<Long> alreadyProvidedIds = scheduledApplications.stream()
                .filter(application -> !blacklistExcludedSet.contains(application.getResearchAppSeq()))
                .filter(application -> !isUnprovided(application))
                .map(ResearchApplication::getResearchAppSeq)
                .distinct()
                .toList();
        Set<Long> alreadyProvidedSet = new LinkedHashSet<>(alreadyProvidedIds);
        Set<Long> claimedIds = claimedApplicationIds(sendJob.getResearchNo());
        List<Long> includedIds = scheduledApplicationIds.stream()
                .filter(applicationId -> !blacklistExcludedSet.contains(applicationId))
                .filter(applicationId -> !alreadyProvidedSet.contains(applicationId))
                .filter(applicationId -> !claimedIds.contains(applicationId))
                .toList();
        return new LegacyResearchScheduledTargetSnapshot(includedIds, blacklistExcludedIds, alreadyProvidedIds);
    }

    public LegacyResearchMailSnapshot loadThresholdSnapshot(Long researchNo) {
        Set<Long> alreadySentIds = new LinkedHashSet<>(
                adminMailSendTargetMapper.findSentApplicationIdsByResearchNoAndTriggerPrefix(researchNo, "LEGACY_")
        );
        Set<Long> claimedIds = claimedApplicationIds(researchNo);
        List<Long> includedApplicationIds = new ArrayList<>();
        int blacklistExcludedCount = 0;
        for (ResearchApplication application : researchApplicationMapper.findAllByResearchNo(researchNo)) {
            if (claimedIds.contains(application.getResearchAppSeq())) {
                continue;
            }
            if (alreadySentIds.contains(application.getResearchAppSeq())) {
                continue;
            }
            if (isBlacklisted(application)) {
                blacklistExcludedCount++;
                continue;
            }
            if (!isUnprovided(application)) {
                continue;
            }
            includedApplicationIds.add(application.getResearchAppSeq());
        }
        return new LegacyResearchMailSnapshot(includedApplicationIds, blacklistExcludedCount);
    }

    public LegacyResearchRecipientSelection parseRecipients(ResearchMaster researchMaster) {
        List<String> rawValues = new ArrayList<>();
        addEmailMatches(rawValues, researchMaster.getRemark());
        addEmailMatches(rawValues, researchMaster.getContactNo());

        Set<String> deduplicatedKeys = new LinkedHashSet<>();
        List<String> recipients = new ArrayList<>();
        int excludedCount = 0;
        for (String value : rawValues) {
            String normalized = value.trim();
            if (!isValidEmail(normalized)) {
                excludedCount++;
                continue;
            }
            String key = normalized.toLowerCase(Locale.ROOT);
            if (!deduplicatedKeys.add(key)) {
                excludedCount++;
                continue;
            }
            recipients.add(normalized);
        }
        String targetName = researchMaster.getCompanyName() == null || researchMaster.getCompanyName().isBlank()
                ? "거래처"
                : researchMaster.getCompanyName().trim();
        return new LegacyResearchRecipientSelection(recipients, excludedCount, targetName);
    }

    private List<Long> scheduledApplicationIds(Long sendJobId) {
        return adminMailSendTargetMapper.findBySendJobId(sendJobId).stream()
                .map(AdminMailSendTarget::getApplicationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Set<Long> claimedApplicationIds(Long researchNo) {
        List<Long> claimedIds = adminMailApplicationClaimMapper.findClaimedApplicationIds(researchNo);
        return new LinkedHashSet<>(claimedIds == null ? List.of() : claimedIds);
    }

    private boolean isBlacklisted(ResearchApplication application) {
        if (application == null) {
            return false;
        }
        String contact = normalizeDigits(application.getAppHphone());
        if (contact == null) {
            contact = normalizeDigits(application.getAppTele());
        }
        return legacyBlacklistMapper.countActiveMatch(
                trimToNull(application.getAppName()),
                normalizeDigits(application.getAppBirth()),
                contact
        ) > 0;
    }

    private boolean isUnprovided(ResearchApplication application) {
        return application != null && !"Y".equalsIgnoreCase(application.getProvideYn());
    }

    private void addEmailMatches(List<String> target, String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(source);
        while (matcher.find()) {
            target.add(matcher.group());
        }
    }

    private boolean isValidEmail(String value) {
        try {
            InternetAddress address = new InternetAddress(value);
            address.validate();
            return value.equals(address.getAddress());
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeDigits(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replaceAll("\\D", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
