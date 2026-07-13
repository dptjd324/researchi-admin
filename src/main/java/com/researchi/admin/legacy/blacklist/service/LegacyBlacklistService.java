package com.researchi.admin.legacy.blacklist.service;

import com.researchi.admin.common.support.PhoneNumberFormatter;
import com.researchi.admin.legacy.blacklist.domain.Blacklist;
import com.researchi.admin.legacy.blacklist.mapper.LegacyBlacklistMapper;
import com.researchi.admin.legacy.blacklist.web.LegacyBlacklistForm;
import com.researchi.admin.legacy.revision.service.LegacyRevisionLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegacyBlacklistService {

    private final LegacyBlacklistMapper legacyBlacklistMapper;
    private final LegacyRevisionLogService legacyRevisionLogService;

    public LegacyBlacklistService(
            LegacyBlacklistMapper legacyBlacklistMapper,
            LegacyRevisionLogService legacyRevisionLogService
    ) {
        this.legacyBlacklistMapper = legacyBlacklistMapper;
        this.legacyRevisionLogService = legacyRevisionLogService;
    }

    public Blacklist getBlacklist(Long blacklistNo) {
        if (blacklistNo == null) {
            return null;
        }
        return legacyBlacklistMapper.findByBlacklistNo(blacklistNo);
    }

    public Blacklist requireBlacklist(Long blacklistNo) {
        Blacklist blacklist = getBlacklist(blacklistNo);
        if (blacklist == null) {
            throw new IllegalArgumentException("Blacklist row not found. blacklistNo=" + blacklistNo);
        }
        return blacklist;
    }

    public List<Blacklist> getPage(String keyword, String birth, String name, String blackYn, int limit, int offset) {
        return legacyBlacklistMapper.findPage(trimToNull(keyword), trimToNull(birth), trimToNull(name), normalizeBlackYnOrNull(blackYn), safeLimit(limit), safeOffset(offset));
    }

    public List<Blacklist> getAll(String keyword, String birth, String name, String blackYn) {
        int totalCount = count(keyword, birth, name, blackYn);
        if (totalCount < 1) {
            return List.of();
        }
        return legacyBlacklistMapper.findPage(
                trimToNull(keyword),
                trimToNull(birth),
                trimToNull(name),
                normalizeBlackYnOrNull(blackYn),
                totalCount,
                0
        );
    }

    public int count(String keyword, String birth, String name, String blackYn) {
        return legacyBlacklistMapper.count(trimToNull(keyword), trimToNull(birth), trimToNull(name), normalizeBlackYnOrNull(blackYn));
    }

    public Long save(LegacyBlacklistForm form, Long changedBy) {
        if (form.getBlacklistNo() == null) {
            Blacklist blacklist = form.toBlacklist(legacyBlacklistMapper.findNextBlacklistNo());
            blacklist.setBlackYn(normalizeBlackYnOrDefault(form.getBlackYn()));
            blacklist.setBlackUserContact(PhoneNumberFormatter.formatForDisplay(blacklist.getBlackUserContact()));
            legacyBlacklistMapper.insert(blacklist);
            return blacklist.getBlacklistNo();
        }

        Blacklist before = requireBlacklist(form.getBlacklistNo());
        legacyRevisionLogService.backupBeforeUpdate("TB_BLACKLIST_MST", String.valueOf(form.getBlacklistNo()), before, changedBy);
        Blacklist update = form.toBlacklist(form.getBlacklistNo());
        update.setBlackYn(normalizeBlackYnOrDefault(form.getBlackYn()));
        update.setBlackUserContact(PhoneNumberFormatter.formatForDisplay(update.getBlackUserContact()));
        int updated = legacyBlacklistMapper.update(update);
        if (updated != 1) {
            throw new IllegalStateException("Failed to update blacklist. blacklistNo=" + form.getBlacklistNo());
        }
        return form.getBlacklistNo();
    }

    public void updateBlackYn(Long blacklistNo, String blackYn, Long changedBy) {
        String normalizedBlackYn = normalizeBlackYnOrDefault(blackYn);
        Blacklist before = requireBlacklist(blacklistNo);
        legacyRevisionLogService.backupBeforeUpdate("TB_BLACKLIST_MST", String.valueOf(blacklistNo), before, changedBy);
        int updated = legacyBlacklistMapper.updateBlackYn(blacklistNo, normalizedBlackYn);
        if (updated != 1) {
            throw new IllegalStateException("Failed to update blacklist status. blacklistNo=" + blacklistNo);
        }
    }

    public void delete(Long blacklistNo, Long changedBy) {
        Blacklist before = requireBlacklist(blacklistNo);
        legacyRevisionLogService.backupBeforeUpdate("TB_BLACKLIST_MST", String.valueOf(blacklistNo), before, changedBy);
        int deleted = legacyBlacklistMapper.deleteByBlacklistNo(blacklistNo);
        if (deleted != 1) {
            throw new IllegalStateException("Failed to delete blacklist. blacklistNo=" + blacklistNo);
        }
    }

    private String normalizeBlackYnOrDefault(String value) {
        String normalized = normalizeBlackYnOrNull(value);
        return normalized == null ? "Y" : normalized;
    }

    private String normalizeBlackYnOrNull(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new IllegalArgumentException("blackYn must be Y or N.");
        }
        return normalized;
    }

    private int safeLimit(int limit) {
        if (limit < 1) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private int safeOffset(int offset) {
        return Math.max(offset, 0);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
