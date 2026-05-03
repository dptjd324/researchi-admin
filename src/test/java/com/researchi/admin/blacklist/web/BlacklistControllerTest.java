package com.researchi.admin.blacklist.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.domain.BlacklistPageData;
import com.researchi.admin.blacklist.service.BlacklistModePolicy;
import com.researchi.admin.blacklist.service.BlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.validation.BeanPropertyBindingResult;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistControllerTest {

    @Mock
    private BlacklistService blacklistService;

    @InjectMocks
    private BlacklistController blacklistController;

    @Test
    void blacklistPopulatesModel() {
        when(blacklistService.getPageData(5L, "kim", "Y", BlacklistModePolicy.PERMANENT_BLOCK))
                .thenReturn(new BlacklistPageData(List.of(), List.of(), List.of()));
        when(blacklistService.getAllowedModes()).thenReturn(List.of(BlacklistModePolicy.PERMANENT_BLOCK));
        when(blacklistService.getAllowedActiveStatuses()).thenReturn(List.of("Y", "N"));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = blacklistController.blacklist(
                5L,
                "kim",
                "Y",
                BlacklistModePolicy.PERMANENT_BLOCK,
                null,
                model,
                new MockHttpServletRequest("GET", "/blacklist"),
                null
        );

        assertThat(viewName).isEqualTo("blacklist/list");
        assertThat(model.get("entries")).isEqualTo(List.of());
        assertThat(model.get("modeOptions")).isEqualTo(List.of(BlacklistModePolicy.PERMANENT_BLOCK));
        assertThat(model.get("currentPage")).isEqualTo(1);
    }

    @Test
    void toggleRedirectsToBlacklistPage() {
        doNothing().when(blacklistService).updateActiveStatus(any(), any(), any(), any());

        String viewName = blacklistController.toggle(
                8L,
                "N",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(viewName).isEqualTo("redirect:/blacklist?id=8&statusUpdated");
        verify(blacklistService).updateActiveStatus(any(), any(), any(), any());
    }

    @Test
    void deleteRedirectsToBlacklistPageAfterRemovingEntry() {
        doNothing().when(blacklistService).remove(any(), any(), any());

        String viewName = blacklistController.delete(
                8L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(viewName).isEqualTo("redirect:/blacklist?deleted");
        verify(blacklistService).remove(any(), any(), any());
    }

    @Test
    void saveRedirectsToBlankFormAfterCreatingNewEntry() {
        BlacklistForm form = new BlacklistForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        when(blacklistService.save(any(), any(), any())).thenReturn(12L);

        String viewName = blacklistController.save(
                form,
                bindingResult,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                new ExtendedModelMap(),
                null
        );

        assertThat(viewName).isEqualTo("redirect:/blacklist?saved");
        verify(blacklistService).save(any(), any(), any());
    }
}
