package com.researchi.admin.blacklist.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.mapper.AdminApplicationQueryMapper;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.domain.BlacklistActionLogItem;
import com.researchi.admin.blacklist.domain.BlacklistEntry;
import com.researchi.admin.blacklist.domain.BlacklistMatchLogItem;
import com.researchi.admin.blacklist.domain.BlacklistPageData;
import com.researchi.admin.blacklist.mapper.AdminBlacklistAdminMapper;
import com.researchi.admin.blacklist.web.BlacklistForm;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BlacklistService {

    private static final int LOG_LIMIT = 30;
    private static final String RESTORED_APPLICATION_STATUS = "RECEIVED";

    private final AdminBlacklistAdminMapper adminBlacklistAdminMapper;
    private final AdminApplicationQueryMapper adminApplicationQueryMapper;
    private final AdminActionLogService adminActionLogService;
    private final PublicFormProtectionService protectionService;

    public BlacklistService(
            AdminBlacklistAdminMapper adminBlacklistAdminMapper,
            AdminApplicationQueryMapper adminApplicationQueryMapper,
            AdminActionLogService adminActionLogService,
            PublicFormProtectionService protectionService
    ) {
        this.adminBlacklistAdminMapper = adminBlacklistAdminMapper;
        this.adminApplicationQueryMapper = adminApplicationQueryMapper;
        this.adminActionLogService = adminActionLogService;
        this.protectionService = protectionService;
    }

    public BlacklistPageData getPageData(Long selectedId, String keyword, String activeYn, String blackMode) {
        List<BlacklistEntry> entries = findEntries(keyword, activeYn, blackMode);
        return new BlacklistPageData(
                entries,
                limitMatchLogs(adminBlacklistAdminMapper.findRecentMatchLogs(selectedId)),
                limitActionLogs(adminBlacklistAdminMapper.findRecentActionLogs(selectedId == null ? null : String.valueOf(selectedId)))
        );
    }

    public List<BlacklistEntry> findEntries(String keyword, String activeYn, String blackMode) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedActiveYn = normalizeActiveYn(activeYn);
        String normalizedMode = normalizeModeFilter(blackMode);
        return adminBlacklistAdminMapper.findEntries(normalizedKeyword, normalizedActiveYn, normalizedMode);
    }

    public BlacklistEntry getEntry(Long id) {
        return id == null ? null : adminBlacklistAdminMapper.findById(id);
    }

    public List<String> getAllowedModes() {
        return BlacklistModePolicy.allowedModes();
    }

    public List<String> getAllowedActiveStatuses() {
        return List.of("Y", "N");
    }

    public void validate(BlacklistForm form, Errors errors) {
        String mode = BlacklistModePolicy.normalize(form.getBlackMode());
        if (!BlacklistModePolicy.allowedModes().contains(mode)) {
            errors.rejectValue("blackMode", "blackMode.invalid", "지원하는 블랙리스트 모드를 선택해 주세요.");
        }

        BlacklistEntry existing = form.getId() == null ? null : adminBlacklistAdminMapper.findById(form.getId());
        String normalizedPhone = protectionService.normalizePhone(form.getMobilePhone());
        String blackName = trimToNull(form.getBlackName());
        boolean hasBirthDate = form.getBlackBirthDate() != null;
        boolean hasPhoneRule = normalizedPhone != null || (existing != null && existing.hasPhoneRule());
        boolean hasPersonalRule = blackName != null && hasBirthDate;

        if (!hasPhoneRule && !hasPersonalRule) {
            errors.reject("criteria.required", "휴대전화를 입력하거나 이름과 생년월일을 함께 입력해 주세요.");
        }
        if (!hasPhoneRule && (blackName == null) != !hasBirthDate) {
            errors.reject("criteria.partial", "Name and birth date must be entered together when using personal matching.");
        }
        if (BlacklistModePolicy.TEMPORARY_BLOCK.equals(mode)) {
            if (form.getExpiresAt() == null) {
                errors.rejectValue("expiresAt", "expiresAt.required", "기간 차단 날짜를 선택해 주세요.");
            } else if (!form.getExpiresAt().isAfter(LocalDateTime.now())) {
                errors.rejectValue("expiresAt", "expiresAt.future", "기간 차단 날짜는 현재 시각 이후로 선택해 주세요.");
            }
        }
    }

    @Transactional("adminTransactionManager")
    public Long save(BlacklistForm form, AdminPrincipal principal, HttpServletRequest request) {
        BlacklistEntry existing = form.getId() == null ? null : requireEntry(form.getId());
        BlacklistEntry entry = existing == null ? new BlacklistEntry() : existing;
        applyForm(entry, form, existing);

        if (existing == null) {
            entry.setActiveYn("Y");
            entry.setCreatedBy(principal.getId());
            adminBlacklistAdminMapper.insert(entry);
            adminActionLogService.log(
                    principal.getId(),
                    "BLACKLIST_CREATE",
                    "BLACKLIST",
                    String.valueOf(entry.getId()),
                    "블랙리스트 등록: " + displayBlackMode(entry.getBlackMode()),
                    request
            );
            return entry.getId();
        }

        int updated = adminBlacklistAdminMapper.update(entry);
        if (updated != 1) {
            throw new IllegalStateException("블랙리스트 정보를 수정하지 못했습니다.");
        }
        adminActionLogService.log(
                principal.getId(),
                "BLACKLIST_UPDATE",
                "BLACKLIST",
                String.valueOf(entry.getId()),
                "블랙리스트 수정: " + displayBlackMode(entry.getBlackMode()),
                request
        );
        return entry.getId();
    }

    @Transactional("adminTransactionManager")
    public Long registerApplication(Long applicationId, AdminPrincipal principal, HttpServletRequest request) {
        ApplicationRecord application = adminApplicationQueryMapper.findById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("지원서를 찾을 수 없습니다.");
        }
        if ("Y".equals(application.getIsBlacklisted())) {
            return null;
        }

        String normalizedPhone = decryptAndNormalizePhone(application.getMobilePhoneEnc());
        String mobilePhoneHash = normalizedPhone == null ? null : protectionService.phoneHash(normalizedPhone);
        String applicantName = trimToNull(application.getApplicantName());

        BlacklistEntry existing = findExistingActiveEntry(phoneHashCandidates(normalizedPhone), applicantName, application.getBirthDate());
        Long blacklistId = existing == null
                ? createApplicationBlacklist(application, mobilePhoneHash, applicantName, principal, request)
                : existing.getId();

        int updated = adminApplicationQueryMapper.updateBlacklistState(
                applicationId,
                BlacklistModePolicy.applicationStatus(BlacklistModePolicy.PERMANENT_BLOCK),
                BlacklistModePolicy.PERMANENT_BLOCK
        );
        if (updated != 1) {
            throw new IllegalStateException("지원서를 블랙리스트 상태로 변경하지 못했습니다.");
        }
        adminActionLogService.log(
                principal.getId(),
                "APPLICATION_BLACKLIST_REGISTER",
                "APPLICATION",
                String.valueOf(applicationId),
                "지원자 목록에서 블랙리스트 등록",
                request
        );
        return blacklistId;
    }

    @Transactional("adminTransactionManager")
    public void updateActiveStatus(Long id, String activeYn, AdminPrincipal principal, HttpServletRequest request) {
        BlacklistEntry existing = requireEntry(id);
        String normalizedActiveYn = normalizeActiveYn(activeYn);
        if (normalizedActiveYn == null) {
            throw new IllegalArgumentException("지원하지 않는 활성 상태입니다.");
        }
        int updated = adminBlacklistAdminMapper.updateActiveStatus(id, normalizedActiveYn, LocalDateTime.now());
        if (updated != 1) {
            throw new IllegalStateException("블랙리스트 상태를 변경하지 못했습니다.");
        }
        adminActionLogService.log(
                principal.getId(),
                "BLACKLIST_STATUS_UPDATE",
                "BLACKLIST",
                String.valueOf(existing.getId()),
                "블랙리스트 상태 변경: " + ("Y".equals(normalizedActiveYn) ? "활성" : "비활성"),
                request
        );
    }

    @Transactional("adminTransactionManager")
    public void remove(Long id, AdminPrincipal principal, HttpServletRequest request) {
        BlacklistEntry existing = requireEntry(id);
        int restoredApplications = adminApplicationQueryMapper.restoreBlacklistApplications(id, RESTORED_APPLICATION_STATUS);
        int deleted = adminBlacklistAdminMapper.deleteById(id);
        if (deleted != 1) {
            throw new IllegalStateException("블랙리스트 항목을 삭제하지 못했습니다.");
        }
        adminActionLogService.log(
                principal.getId(),
                "BLACKLIST_DELETE",
                "BLACKLIST",
                String.valueOf(existing.getId()),
                "블랙리스트 삭제 및 지원자 복구: " + restoredApplications + "건",
                request
        );
    }

    @Transactional("adminTransactionManager")
    public int expireExpiredEntries(LocalDateTime now) {
        int expiredCount = 0;
        for (BlacklistEntry entry : adminBlacklistAdminMapper.findExpiredActiveEntries(now)) {
            int updated = adminBlacklistAdminMapper.updateActiveStatus(entry.getId(), "N", now);
            if (updated != 1) {
                continue;
            }
            expiredCount++;
            adminActionLogService.log(
                    null,
                    "BLACKLIST_EXPIRE",
                    "BLACKLIST",
                    String.valueOf(entry.getId()),
                    "임시 블랙리스트가 만료되어 자동 비활성화되었습니다.",
                    null
            );
        }
        return expiredCount;
    }

    private void applyForm(BlacklistEntry entry, BlacklistForm form, BlacklistEntry existing) {
        String normalizedPhone = protectionService.normalizePhone(form.getMobilePhone());
        entry.setBlackName(trimToNull(form.getBlackName()));
        entry.setBlackBirthDate(form.getBlackBirthDate());
        entry.setBlackReason(trimToNull(form.getBlackReason()));
        entry.setBlackMode(BlacklistModePolicy.normalize(form.getBlackMode()));
        entry.setExpiresAt(BlacklistModePolicy.TEMPORARY_BLOCK.equals(entry.getBlackMode()) ? form.getExpiresAt() : null);

        if (normalizedPhone != null) {
            entry.setBlackMobilePhoneHash(protectionService.phoneHash(normalizedPhone));
        } else if (existing == null) {
            entry.setBlackMobilePhoneHash(null);
        }
    }

    private Long createApplicationBlacklist(
            ApplicationRecord application,
            String mobilePhoneHash,
            String applicantName,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        if (mobilePhoneHash == null && (applicantName == null || application.getBirthDate() == null)) {
            throw new IllegalStateException("블랙리스트 등록에 필요한 휴대전화 또는 이름/생년월일 정보가 없습니다.");
        }
        BlacklistEntry entry = new BlacklistEntry();
        entry.setBlackName(applicantName);
        entry.setBlackBirthDate(application.getBirthDate());
        entry.setBlackMobilePhoneHash(mobilePhoneHash);
        entry.setBlackReason("지원자 목록에서 수동 등록");
        entry.setBlackMode(BlacklistModePolicy.PERMANENT_BLOCK);
        entry.setActiveYn("Y");
        entry.setCreatedBy(principal.getId());
        adminBlacklistAdminMapper.insert(entry);
        adminActionLogService.log(
                principal.getId(),
                "BLACKLIST_CREATE",
                "BLACKLIST",
                String.valueOf(entry.getId()),
                "지원자 목록에서 블랙리스트 등록",
                request
        );
        return entry.getId();
    }

    private BlacklistEntry findExistingActiveEntry(List<String> mobilePhoneHashes, String applicantName, java.time.LocalDate birthDate) {
        if (mobilePhoneHashes != null && !mobilePhoneHashes.isEmpty()) {
            BlacklistEntry existing = adminBlacklistAdminMapper.findActiveByMobilePhoneHashes(mobilePhoneHashes);
            if (existing != null) {
                return existing;
            }
        }
        if (applicantName != null && birthDate != null) {
            return adminBlacklistAdminMapper.findActiveByNameAndBirthDate(applicantName, birthDate);
        }
        return null;
    }

    private List<String> phoneHashCandidates(String normalizedPhone) {
        String currentHash = protectionService.phoneHash(normalizedPhone);
        String legacyHash = protectionService.legacyPhoneHash(normalizedPhone);
        if (currentHash == null) {
            return List.of();
        }
        if (currentHash.equals(legacyHash)) {
            return List.of(currentHash);
        }
        return List.of(currentHash, legacyHash);
    }

    private String decryptAndNormalizePhone(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isBlank()) {
            return null;
        }
        return protectionService.normalizePhone(protectionService.decrypt(encryptedPhone));
    }

    private BlacklistEntry requireEntry(Long id) {
        BlacklistEntry entry = adminBlacklistAdminMapper.findById(id);
        if (entry == null) {
            throw new IllegalArgumentException("블랙리스트 정보를 찾을 수 없습니다.");
        }
        return entry;
    }

    private String displayBlackMode(String blackMode) {
        if (BlacklistModePolicy.TEMPORARY_BLOCK.equals(blackMode)) {
            return "임시 차단";
        }
        if (BlacklistModePolicy.PERMANENT_BLOCK.equals(blackMode)) {
            return "영구 차단";
        }
        if (BlacklistModePolicy.MANUAL_REVIEW.equals(blackMode)) {
            return "관리자 검토";
        }
        return blackMode;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeActiveYn(String activeYn) {
        String normalized = trimToNull(activeYn);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return List.of("Y", "N").contains(normalized) ? normalized : null;
    }

    private String normalizeModeFilter(String blackMode) {
        String normalized = trimToNull(blackMode);
        if (normalized == null) {
            return null;
        }
        normalized = BlacklistModePolicy.normalize(normalized);
        return BlacklistModePolicy.allowedModes().contains(normalized) ? normalized : null;
    }

    private List<BlacklistMatchLogItem> limitMatchLogs(List<BlacklistMatchLogItem> logs) {
        return logs.size() <= LOG_LIMIT ? logs : logs.subList(0, LOG_LIMIT);
    }

    private List<BlacklistActionLogItem> limitActionLogs(List<BlacklistActionLogItem> logs) {
        return logs.size() <= LOG_LIMIT ? logs : logs.subList(0, LOG_LIMIT);
    }
}
