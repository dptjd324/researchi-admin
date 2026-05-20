package com.researchi.admin.client.web;

import com.researchi.admin.client.domain.ClientImpactSummary;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.service.ClientImpactService;
import com.researchi.admin.client.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientImpactService clientImpactService;

    public ClientController(
            ClientService clientService,
            ClientImpactService clientImpactService
    ) {
        this.clientService = clientService;
        this.clientImpactService = clientImpactService;
    }

    @GetMapping
    public String clients(
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "active", required = false) String active,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        if (clientId != null) {
            return "redirect:/clients/" + clientId + "/edit";
        }
        populateListModel(model, keyword, active, csrfToken);
        return "clients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, CsrfToken csrfToken) {
        populateFormModel(model, new ClientForm(), "거래처 등록", null, csrfToken);
        return "clients/form";
    }

    @GetMapping("/{clientId}/edit")
    public String editForm(
            @PathVariable Long clientId,
            Model model,
            CsrfToken csrfToken
    ) {
        populateFormModel(model, clientService.toForm(clientId), "거래처 수정", clientImpactService.summarize(clientId), csrfToken);
        return "clients/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("clientForm") ClientForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        ClientImpactSummary impactSummary = form.getId() == null
                ? new ClientImpactSummary(null, 0, List.of())
                : clientImpactService.summarize(form.getId());
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, form.getId() == null ? "거래처 등록" : "거래처 수정", impactSummary, resolveCsrfToken(request));
            return "clients/form";
        }
        try {
            Long clientId = clientService.save(form);
            return "redirect:/clients/" + clientId + "/edit?saved";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("clientError", ex.getMessage());
            populateFormModel(model, form, form.getId() == null ? "거래처 등록" : "거래처 수정", impactSummary, resolveCsrfToken(request));
            return "clients/form";
        }
    }

    @PostMapping("/{clientId}/delete")
    public String delete(@PathVariable Long clientId) {
        clientService.deleteClient(clientId);
        return "redirect:/clients?deleted";
    }

    private void populateListModel(Model model, String keyword, String active, CsrfToken csrfToken) {
        List<ClientSummary> clients = filterClients(clientService.getAllClientSummaries(), keyword, active);
        model.addAttribute("pageTitle", "거래처");
        model.addAttribute("pageDescription", "거래처를 조회, 등록하며 관리합니다.");
        model.addAttribute("clients", clients);
        model.addAttribute("totalItemCount", clients.size());
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active == null || active.isBlank() ? "ALL" : active);
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    private void populateFormModel(
            Model model,
            ClientForm form,
            String title,
            ClientImpactSummary impactSummary,
            CsrfToken csrfToken
    ) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageDescription", "거래처 기본 정보와 수신 이메일을 관리합니다.");
        model.addAttribute("clientForm", form);
        model.addAttribute("impactSummary", impactSummary == null ? new ClientImpactSummary(form.getId(), 0, List.of()) : impactSummary);
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    private List<ClientSummary> filterClients(List<ClientSummary> clients, String keyword, String active) {
        String normalizedKeyword = normalize(keyword);
        String activeFilter = active == null || active.isBlank() ? "ALL" : active.trim().toUpperCase(Locale.ROOT);
        return clients.stream()
                .filter(client -> "ALL".equals(activeFilter)
                        || ("Y".equals(activeFilter) && client.active())
                        || ("N".equals(activeFilter) && !client.active()))
                .filter(client -> normalizedKeyword == null || contains(client.clientName(), normalizedKeyword)
                        || contains(client.departmentName(), normalizedKeyword)
                        || contains(client.primaryContactName(), normalizedKeyword)
                        || contains(client.primaryContactNo(), normalizedKeyword)
                        || contains(client.primaryEmail(), normalizedKeyword))
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private CsrfToken resolveCsrfToken(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            return csrfToken;
        }
        Object fallback = request.getAttribute("_csrf");
        return fallback instanceof CsrfToken csrfToken ? csrfToken : null;
    }
}
