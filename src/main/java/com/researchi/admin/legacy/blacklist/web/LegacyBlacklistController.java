package com.researchi.admin.legacy.blacklist.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.legacy.blacklist.service.LegacyBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/legacy-blacklist")
public class LegacyBlacklistController {

    private final LegacyBlacklistService legacyBlacklistService;

    public LegacyBlacklistController(LegacyBlacklistService legacyBlacklistService) {
        this.legacyBlacklistService = legacyBlacklistService;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "birth", required = false) String birth,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "blackYn", required = false) String blackYn,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        populateModel(model, keyword, birth, name, blackYn, csrfToken);
        return "legacy-blacklist/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, CsrfToken csrfToken) {
        populateFormModel(model, LegacyBlacklistForm.from(null), csrfToken);
        return "legacy-blacklist/form";
    }

    @GetMapping("/{blacklistNo}/edit")
    public String editForm(
            @PathVariable Long blacklistNo,
            Model model,
            CsrfToken csrfToken
    ) {
        populateFormModel(model, LegacyBlacklistForm.from(legacyBlacklistService.requireBlacklist(blacklistNo)), csrfToken);
        return "legacy-blacklist/form";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("form") LegacyBlacklistForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, csrfToken);
            return "legacy-blacklist/form";
        }
        Long blacklistNo = legacyBlacklistService.save(form, principal == null ? null : principal.getId());
        return "redirect:/legacy-blacklist?blacklistNo=" + blacklistNo + "&saved";
    }

    @PostMapping("/{blacklistNo}/status")
    public String updateStatus(
            @PathVariable Long blacklistNo,
            @RequestParam("blackYn") String blackYn,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        legacyBlacklistService.updateBlackYn(blacklistNo, blackYn, principal == null ? null : principal.getId());
        return "redirect:/legacy-blacklist?blacklistNo=" + blacklistNo + "&statusUpdated";
    }

    @PostMapping("/{blacklistNo}/delete")
    public String delete(
            @PathVariable Long blacklistNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        legacyBlacklistService.delete(blacklistNo, principal == null ? null : principal.getId());
        return "redirect:/legacy-blacklist?deleted";
    }

    private void populateModel(
            Model model,
            String keyword,
            String birth,
            String name,
            String blackYn,
            CsrfToken csrfToken
    ) {
        int totalCount = legacyBlacklistService.count(keyword, birth, name, blackYn);
        model.addAttribute("pageTitle", "블랙리스트");
        model.addAttribute("pageDescription", "블랙리스트를 조회,등록하며 관리합니다");
        model.addAttribute("entries", legacyBlacklistService.getAll(keyword, birth, name, blackYn));
        model.addAttribute("totalItemCount", totalCount);
        model.addAttribute("keyword", keyword);
        model.addAttribute("birth", birth);
        model.addAttribute("name", name);
        model.addAttribute("blackYn", blackYn);
        model.addAttribute("_csrf", csrfToken);
    }

    private void populateFormModel(Model model, LegacyBlacklistForm form, CsrfToken csrfToken) {
        model.addAttribute("pageTitle", form.getBlacklistNo() == null ? "블랙리스트 등록" : "블랙리스트 수정");
        model.addAttribute("pageDescription", "블랙리스트를 조회,등록하며 관리합니다");
        model.addAttribute("form", form);
        model.addAttribute("_csrf", csrfToken);
    }
}
