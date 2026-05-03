package com.researchi.admin.blacklist.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.domain.BlacklistEntry;
import com.researchi.admin.blacklist.domain.BlacklistPageData;
import com.researchi.admin.blacklist.service.BlacklistService;
import com.researchi.admin.common.web.PaginationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/blacklist")
public class BlacklistController {

    private final BlacklistService blacklistService;

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping
    public String blacklist(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "activeYn", required = false) String activeYn,
            @RequestParam(name = "blackMode", required = false) String blackMode,
            @RequestParam(name = "page", required = false) Integer page,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        BlacklistForm form = toForm(blacklistService.getEntry(id));
        populateModel(model, form, id, keyword, activeYn, blackMode, csrfToken, request, page);
        return "blacklist/list";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") BlacklistForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        blacklistService.validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateModel(model, form, form.getId(), null, null, null, csrfToken, request, null);
            return "blacklist/list";
        }
        boolean isNewEntry = form.getId() == null;
        Long savedId = blacklistService.save(form, principal, request);
        if (isNewEntry) {
            return "redirect:/blacklist?saved";
        }
        return "redirect:/blacklist?id=" + savedId + "&saved";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            @RequestParam("activeYn") String activeYn,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        blacklistService.updateActiveStatus(id, activeYn, principal, request);
        return "redirect:/blacklist?id=" + id + "&statusUpdated";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        blacklistService.remove(id, principal, request);
        return "redirect:/blacklist?deleted";
    }

    private void populateModel(
            Model model,
            BlacklistForm form,
            Long selectedId,
            String keyword,
            String activeYn,
            String blackMode,
            CsrfToken csrfToken,
            HttpServletRequest request,
            Integer page
    ) {
        BlacklistPageData pageData = blacklistService.getPageData(selectedId, keyword, activeYn, blackMode);
        model.addAttribute("pageTitle", "블랙리스트");
        model.addAttribute("pageDescription", "블랙리스트 규칙 관리, 차단 정책, 최근 매칭 로그와 관리자 액션 로그를 확인합니다.");
        model.addAttribute("form", form);
        model.addAttribute("entries", PaginationSupport.apply(model, request, pageData.entries(), page, PaginationSupport.DEFAULT_PAGE_SIZE));
        model.addAttribute("matchLogs", pageData.matchLogs());
        model.addAttribute("actionLogs", pageData.actionLogs());
        model.addAttribute("selectedId", selectedId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeYn", activeYn);
        model.addAttribute("blackMode", blackMode);
        model.addAttribute("modeOptions", blacklistService.getAllowedModes());
        model.addAttribute("activeOptions", blacklistService.getAllowedActiveStatuses());
        model.addAttribute("_csrf", csrfToken);
    }

    private BlacklistForm toForm(BlacklistEntry selectedEntry) {
        BlacklistForm form = new BlacklistForm();
        if (selectedEntry == null) {
            return form;
        }
        form.setId(selectedEntry.getId());
        form.setBlackName(selectedEntry.getBlackName());
        form.setBlackBirthDate(selectedEntry.getBlackBirthDate());
        form.setBlackReason(selectedEntry.getBlackReason());
        form.setBlackMode(selectedEntry.getBlackMode());
        form.setExpiresAt(selectedEntry.getExpiresAt());
        return form;
    }
}
