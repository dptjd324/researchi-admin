package com.researchi.admin.blacklist.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.mapper.AdminApplicationQueryMapper;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.domain.BlacklistEntry;
import com.researchi.admin.blacklist.mapper.AdminBlacklistAdminMapper;
import com.researchi.admin.blacklist.web.BlacklistForm;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.publicform.config.PublicFormProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private AdminBlacklistAdminMapper adminBlacklistAdminMapper;

    @Mock
    private AdminApplicationQueryMapper adminApplicationQueryMapper;

    @Mock
    private AdminActionLogService adminActionLogService;

    private BlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        blacklistService = new BlacklistService(
                adminBlacklistAdminMapper,
                adminApplicationQueryMapper,
                adminActionLogService,
                new PublicFormProtectionService(properties)
        );
    }

    @Test
    void validateRequiresExpiryForTemporaryBlock() {
        BlacklistForm form = new BlacklistForm();
        form.setMobilePhone("010-1234-5678");
        form.setBlackMode(BlacklistModePolicy.TEMPORARY_BLOCK);
        form.setBlackReason("temp");

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        blacklistService.validate(form, errors);

        assertThat(errors.hasFieldErrors("expiresAt")).isTrue();
    }

    @Test
    void saveHashesPhoneAndWritesCreateActionLog() {
        BlacklistForm form = new BlacklistForm();
        form.setBlackName("Kim");
        form.setBlackBirthDate(LocalDate.of(1990, 5, 1));
        form.setMobilePhone("010-1234-5678");
        form.setBlackMode(BlacklistModePolicy.PERMANENT_BLOCK);
        form.setBlackReason("policy");

        doAnswer(invocation -> {
            BlacklistEntry entry = invocation.getArgument(0);
            entry.setId(15L);
            return null;
        }).when(adminBlacklistAdminMapper).insert(any(BlacklistEntry.class));

        Long savedId = blacklistService.save(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(savedId).isEqualTo(15L);
        ArgumentCaptor<BlacklistEntry> captor = ArgumentCaptor.forClass(BlacklistEntry.class);
        verify(adminBlacklistAdminMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
        assertThat(captor.getValue().getBlackMobilePhoneHash()).hasSize(64);
        assertThat(captor.getValue().getBlackMobilePhoneHash()).isNotEqualTo("01012345678");
        verify(adminActionLogService).log(eq(1L), eq("BLACKLIST_CREATE"), eq("BLACKLIST"), eq("15"), eq("블랙리스트 등록: 영구 차단"), any());
    }

    @Test
    void saveUpdatesExistingBlacklistEntry() {
        BlacklistForm form = new BlacklistForm();
        form.setId(21L);
        form.setBlackName("Updated Kim");
        form.setBlackBirthDate(LocalDate.of(1992, 2, 2));
        form.setBlackMode(BlacklistModePolicy.MANUAL_REVIEW);
        form.setBlackReason("updated");

        BlacklistEntry existing = new BlacklistEntry();
        existing.setId(21L);
        existing.setBlackMobilePhoneHash("existing-hash");
        when(adminBlacklistAdminMapper.findById(21L)).thenReturn(existing);
        when(adminBlacklistAdminMapper.update(any(BlacklistEntry.class))).thenReturn(1);

        Long savedId = blacklistService.save(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(savedId).isEqualTo(21L);
        ArgumentCaptor<BlacklistEntry> captor = ArgumentCaptor.forClass(BlacklistEntry.class);
        verify(adminBlacklistAdminMapper).update(captor.capture());
        assertThat(captor.getValue().getBlackName()).isEqualTo("Updated Kim");
        assertThat(captor.getValue().getBlackMode()).isEqualTo(BlacklistModePolicy.MANUAL_REVIEW);
        assertThat(captor.getValue().getBlackMobilePhoneHash()).isEqualTo("existing-hash");
        verify(adminActionLogService).log(eq(1L), eq("BLACKLIST_UPDATE"), eq("BLACKLIST"), eq("21"), eq("블랙리스트 수정: 관리자 검토"), any());
    }

    @Test
    void registerApplicationCreatesBlacklistAndMarksApplicationBlocked() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        String encryptedPhone = protectionService.encrypt("010-1234-5678");
        ApplicationRecord application = new ApplicationRecord();
        application.setId(90L);
        application.setApplicantName("Kim");
        application.setBirthDate(LocalDate.of(1990, 5, 1));
        application.setMobilePhoneEnc(encryptedPhone);
        application.setIsBlacklisted("N");
        when(adminApplicationQueryMapper.findById(90L)).thenReturn(application);
        when(adminApplicationQueryMapper.updateBlacklistState(90L, "BLOCKED", BlacklistModePolicy.PERMANENT_BLOCK)).thenReturn(1);
        doAnswer(invocation -> {
            BlacklistEntry entry = invocation.getArgument(0);
            entry.setId(91L);
            return null;
        }).when(adminBlacklistAdminMapper).insert(any(BlacklistEntry.class));

        Long blacklistId = blacklistService.registerApplication(
                90L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(blacklistId).isEqualTo(91L);
        verify(adminApplicationQueryMapper).updateBlacklistState(90L, "BLOCKED", BlacklistModePolicy.PERMANENT_BLOCK);
        ArgumentCaptor<BlacklistEntry> captor = ArgumentCaptor.forClass(BlacklistEntry.class);
        verify(adminBlacklistAdminMapper).insert(captor.capture());
        assertThat(captor.getValue().getBlackMode()).isEqualTo(BlacklistModePolicy.PERMANENT_BLOCK);
        assertThat(captor.getValue().getBlackMobilePhoneHash()).hasSize(64);
    }

    @Test
    void updateActiveStatusWritesStatusActionLog() {
        BlacklistEntry entry = new BlacklistEntry();
        entry.setId(7L);
        when(adminBlacklistAdminMapper.findById(7L)).thenReturn(entry);
        when(adminBlacklistAdminMapper.updateActiveStatus(eq(7L), eq("N"), any())).thenReturn(1);

        blacklistService.updateActiveStatus(
                7L,
                "N",
                new AdminPrincipal(2L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(adminBlacklistAdminMapper).updateActiveStatus(eq(7L), eq("N"), any());
        verify(adminActionLogService).log(eq(2L), eq("BLACKLIST_STATUS_UPDATE"), eq("BLACKLIST"), eq("7"), eq("블랙리스트 상태 변경: 비활성"), any());
    }

    @Test
    void getPageDataReturnsBlacklistListForPhase7ListItem() {
        BlacklistEntry entry = new BlacklistEntry();
        entry.setId(4L);
        entry.setBlackName("Kim");
        when(adminBlacklistAdminMapper.findEntries("kim", "Y", BlacklistModePolicy.PERMANENT_BLOCK)).thenReturn(List.of(entry));
        when(adminBlacklistAdminMapper.findRecentMatchLogs(4L)).thenReturn(List.of());
        when(adminBlacklistAdminMapper.findRecentActionLogs("4")).thenReturn(List.of());

        assertThat(blacklistService.getPageData(4L, "kim", "Y", BlacklistModePolicy.PERMANENT_BLOCK).entries())
                .extracting(BlacklistEntry::getId)
                .containsExactly(4L);
    }

    @Test
    void getPageDataLimitsLogCollections() {
        when(adminBlacklistAdminMapper.findEntries(null, null, null)).thenReturn(List.of());
        when(adminBlacklistAdminMapper.findRecentMatchLogs(null)).thenReturn(List.of());
        when(adminBlacklistAdminMapper.findRecentActionLogs(null)).thenReturn(List.of());

        assertThat(blacklistService.getPageData(null, null, null, null).entries()).isEmpty();
    }

    @Test
    void expireExpiredEntriesDeactivatesOnlyExpiredActiveEntries() {
        BlacklistEntry expired = new BlacklistEntry();
        expired.setId(31L);
        when(adminBlacklistAdminMapper.findExpiredActiveEntries(any())).thenReturn(List.of(expired));
        when(adminBlacklistAdminMapper.updateActiveStatus(eq(31L), eq("N"), any())).thenReturn(1);

        int expiredCount = blacklistService.expireExpiredEntries(LocalDateTime.of(2026, 4, 16, 15, 0));

        assertThat(expiredCount).isEqualTo(1);
        verify(adminBlacklistAdminMapper).updateActiveStatus(eq(31L), eq("N"), any());
        verify(adminActionLogService).log(
                eq(null),
                eq("BLACKLIST_EXPIRE"),
                eq("BLACKLIST"),
                eq("31"),
                eq("임시 블랙리스트가 만료되어 자동 비활성화되었습니다."),
                eq(null)
        );
    }
}
