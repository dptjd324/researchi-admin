package com.researchi.admin.client.web;

import com.researchi.admin.client.domain.ClientImpactSummary;
import com.researchi.admin.client.domain.ClientMigrationPreview;
import com.researchi.admin.client.domain.ClientMigrationResult;
import com.researchi.admin.client.service.ClientImpactService;
import com.researchi.admin.client.service.ClientMigrationService;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.common.web.PaginationSupport;
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

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientImpactService clientImpactService;
    private final ClientMigrationService clientMigrationService;

    public ClientController(
            ClientService clientService,
            ClientImpactService clientImpactService,
            ClientMigrationService clientMigrationService
    ) {
        this.clientService = clientService;
        this.clientImpactService = clientImpactService;
        this.clientMigrationService = clientMigrationService;
    }

    @GetMapping
    public String clients(
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "page", required = false) Integer page,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ClientForm form = clientId == null ? new ClientForm() : clientService.toForm(clientId);
        populateModel(model, form, clientId, csrfToken, null, request, page);
        return "clients/list";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("clientForm") ClientForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        ClientImpactSummary impactSummary = form.getId() == null
                ? new ClientImpactSummary(null, 0, java.util.List.of())
                : clientImpactService.summarize(form.getId());
        if (Boolean.FALSE.equals(form.getActive()) && impactSummary.hasImpact() && !Boolean.TRUE.equals(form.getConfirmImpact())) {
            bindingResult.reject("clientImpact", "변경되지 않았습니다. 연결된 공고가 있어 미사용 처리 전 영향 확인이 필요합니다.");
            bindingResult.rejectValue("confirmImpact", "clientImpact.confirmRequired", "연결된 공고 영향을 확인 후 다시 저장해 주세요.");
        }
        if (bindingResult.hasErrors()) {
            populateModel(model, form, form.getId(), resolveCsrfToken(request), impactSummary, request, null);
            return "clients/list";
        }
        try {
            Long clientId = clientService.save(form);
            return "redirect:/clients?clientId=" + clientId + "&saved";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("clientError", ex.getMessage());
            populateModel(model, form, form.getId(), resolveCsrfToken(request), impactSummary, request, null);
            return "clients/list";
        }
    }

    @PostMapping("/{clientId}/delete")
    public String delete(
            @PathVariable Long clientId,
            Model model,
            HttpServletRequest request
    ) {
        ClientImpactSummary impactSummary = clientImpactService.summarize(clientId);
        if (impactSummary.hasImpact()) {
            ClientForm form = clientService.toForm(clientId);
            model.addAttribute("deleteError", "연결된 공고가 있어 삭제할 수 없습니다. 먼저 공고의 거래처 연결을 변경하세요.");
            populateModel(model, form, clientId, resolveCsrfToken(request), impactSummary, request, null);
            return "clients/list";
        }
        clientService.deleteClient(clientId);
        return "redirect:/clients?deleted";
    }

    @PostMapping("/migrations/legacy-jobs")
    public String migrateLegacyJobs() {
        ClientMigrationResult result = clientMigrationService.migrateLegacyJobClients();
        return "redirect:/clients?migratedJobs=" + result.migratedJobCount()
                + "&createdClients=" + result.createdClientCount()
                + "&reusedClients=" + result.reusedClientCount();
    }

    private void populateModel(
            Model model,
            ClientForm form,
            Long selectedClientId,
            CsrfToken csrfToken,
            ClientImpactSummary explicitImpactSummary,
            HttpServletRequest request,
            Integer page
    ) {
        ClientImpactSummary impactSummary = explicitImpactSummary != null
                ? explicitImpactSummary
                : (selectedClientId == null ? new ClientImpactSummary(null, 0, java.util.List.of()) : clientImpactService.summarize(selectedClientId));
        ClientMigrationPreview migrationPreview = clientMigrationService.previewLegacyJobMigration();

        model.addAttribute("pageTitle", "거래처 관리");
        model.addAttribute("pageDescription", "공고에 연결할 거래처와 메일 수신 담당자 정보를 관리합니다.");
        model.addAttribute("clientForm", form);
        model.addAttribute(
                "clients",
                PaginationSupport.apply(model, request, clientService.getAllClientSummaries(), page, PaginationSupport.DEFAULT_PAGE_SIZE)
        );
        model.addAttribute("selectedClientId", selectedClientId);
        model.addAttribute("impactSummary", impactSummary);
        model.addAttribute("migrationPreview", migrationPreview);
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
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
