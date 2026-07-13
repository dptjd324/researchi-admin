package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.matching.domain.LegacyEmailSendResult;
import com.researchi.admin.legacy.matching.domain.LegacyKeywordIndexResult;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingHistory;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingOverview;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunStatus;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunTicket;
import com.researchi.admin.legacy.matching.domain.LegacySmsSendLimitExceededException;
import com.researchi.admin.legacy.matching.domain.LegacySmsSendResult;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.legacy.matching.service.LegacyMatchingAsyncExecutor;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/research/{researchNo}/matching")
public class LegacyMatchingController {

    private final LegacyMatchingService legacyMatchingService;
    private final ResearchMasterService researchMasterService;
    private final LegacyMatchingAsyncExecutor legacyMatchingAsyncExecutor;

    public LegacyMatchingController(
            LegacyMatchingService legacyMatchingService,
            ResearchMasterService researchMasterService,
            LegacyMatchingAsyncExecutor legacyMatchingAsyncExecutor
    ) {
        this.legacyMatchingService = legacyMatchingService;
        this.researchMasterService = researchMasterService;
        this.legacyMatchingAsyncExecutor = legacyMatchingAsyncExecutor;
    }

    @GetMapping
    public String detail(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (hasConditionCheckParam(searchForm) && !hasConditionInput(searchForm)) {
            return "redirect:/research/" + researchNo + "/matching?keywordRequired";
        }
        LegacyMatchingOverview overview = legacyMatchingService.getOverview(researchNo, searchForm.toCondition());
        boolean conditionChecked = hasConditionCheckParam(searchForm) && hasConditionInput(searchForm);
        model.addAttribute("pageTitle", "좌담회/설문 매칭");
        model.addAttribute("pageDescription", "수동 키워드 기준으로 신청자 매칭 회차를 실행합니다.");
        model.addAttribute("research", overview.research());
        model.addAttribute("overview", overview);
        model.addAttribute("searchForm", searchForm);
        model.addAttribute("conditionChecked", conditionChecked);
        model.addAttribute("_csrf", csrfToken);
        return "research/matching";
    }

    @PostMapping("/sms")
    public String sendSms(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "selectedApplicationIds", required = false) List<Long> selectedApplicationIds,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        String redirectPath = "run-window".equals(returnTo)
                ? "/matching/run-window-result"
                : "/matching";
        try {
            LegacySmsSendResult result = legacyMatchingService.sendSmsNotifications(
                    researchNo,
                    searchForm.toCondition(),
                    selectedIdSet(selectedApplicationIds),
                    principal,
                    request
            );
            return "redirect:/research/" + researchNo + redirectPath + "?smsSent"
                    + "&targetCount=" + result.targetCount()
                    + "&sentCount=" + result.sentCount()
                    + "&skippedDuplicateCount=" + result.skippedDuplicateCount()
                    + "&failedCount=" + result.failedCount()
                    + queryParams(searchForm);
        } catch (LegacySmsSendLimitExceededException ex) {
            return "redirect:/research/" + researchNo + redirectPath + "?smsLimitExceeded"
                    + queryParam("message", ex.getMessage())
                    + "&requestedCount=" + ex.getRequestedCount()
                    + "&dailySentCount=" + ex.getDailySentCount()
                    + "&dailyLimit=" + ex.getDailyLimit()
                    + "&monthlySentCount=" + ex.getMonthlySentCount()
                    + "&monthlyLimit=" + ex.getMonthlyLimit()
                    + queryParams(searchForm);
        }
    }

    @PostMapping("/email")
    public String sendEmail(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "selectedApplicationIds", required = false) List<Long> selectedApplicationIds,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        LegacyEmailSendResult result = legacyMatchingService.sendEmailNotifications(
                researchNo,
                searchForm.toCondition(),
                selectedIdSet(selectedApplicationIds),
                principal,
                request
        );
        return "redirect:/research/" + researchNo + "/matching/run-window-result?emailSent"
                + "&targetCount=" + result.targetCount()
                + "&emailSentCount=" + result.sentCount()
                + "&emailSkippedDuplicateCount=" + result.skippedDuplicateCount()
                + "&emailFailedCount=" + result.failedCount()
                + queryParams(searchForm);
    }

    @PostMapping("/run")
    public String runMatching(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "conditionChecked", defaultValue = "false") boolean conditionChecked
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!conditionChecked) {
            return missingPrerequisiteRedirect(researchNo, searchForm);
        }
        LegacyKeywordIndexResult result = legacyMatchingService.runMatchingCycle(researchNo, searchForm.toCondition());
        return "redirect:/research/" + researchNo + "/matching?matchingRun"
                + "&cycleNo=" + result.cycleNo()
                + "&indexedApplicationCount=" + result.indexedApplicationCount()
                + "&insertedKeywordCount=" + result.insertedKeywordCount()
                + queryParams(searchForm);
    }

    @PostMapping("/run-window")
    public String runMatchingWindow(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "conditionChecked", defaultValue = "false") boolean conditionChecked,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!conditionChecked) {
            return missingPrerequisiteRedirect(researchNo, searchForm);
        }
        LegacyMatchingRunTicket runTicket = legacyMatchingService.startOrReuseMatchingRun(
                researchNo,
                searchForm.toCondition()
        );
        if ("PENDING".equals(runTicket.status())) {
            try {
                legacyMatchingAsyncExecutor.submit(runTicket.jobId());
            } catch (TaskRejectedException ex) {
                legacyMatchingService.failMatchingRun(
                        runTicket.jobId(),
                        "매칭 실행 대기열이 가득 차 작업을 시작하지 못했습니다."
                );
            }
        }
        model.addAttribute("pageTitle", "매칭 진행 중");
        model.addAttribute("research", researchMasterService.getResearchMaster(researchNo));
        model.addAttribute("searchForm", searchForm);
        model.addAttribute("activeKeywordText", String.join(", ", searchForm.toCondition().displayFilters()));
        model.addAttribute("runTicket", runTicket);
        model.addAttribute("_csrf", csrfToken);
        return "research/matching-progress-window";
    }

    @GetMapping("/run-window/status")
    @ResponseBody
    public ResponseEntity<LegacyMatchingRunStatusResponse> matchingRunStatus(
            @PathVariable Long researchNo,
            @RequestParam("jobId") Long jobId
    ) {
        try {
            LegacyMatchingRunStatus status = legacyMatchingService.getMatchingRunStatus(researchNo, jobId);
            String resultUrl = "/research/" + researchNo + "/matching/run-window-result?jobId=" + status.jobId();
            if (status.conditionStorageKey() != null && !status.conditionStorageKey().isBlank()) {
                resultUrl += "&" + status.conditionStorageKey();
            }
            return ResponseEntity.ok(new LegacyMatchingRunStatusResponse(
                    status.jobId(),
                    status.cycleNo(),
                    status.status(),
                    status.failReason(),
                    resultUrl
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/run-window-result")
    public String runMatchingWindowResult(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "jobId", required = false) Long jobId,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        LegacyMatchingOverview overview = legacyMatchingService.getOverview(researchNo, searchForm.toCondition());
        model.addAttribute("pageTitle", "회차 실행 결과");
        model.addAttribute("research", overview.research());
        model.addAttribute("overview", overview);
        model.addAttribute("searchForm", searchForm);
        LegacyKeywordIndexResult result = jobId == null
                ? new LegacyKeywordIndexResult(0, 0).withCycleNo(overview.latestCycleNo())
                : legacyMatchingService.getCompletedMatchingRunResult(researchNo, jobId);
        model.addAttribute("result", result);
        model.addAttribute("_csrf", csrfToken);
        return "research/matching-run-window";
    }

    @PostMapping("/refresh")
    public String refresh(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm,
            @RequestParam(name = "conditionChecked", defaultValue = "false") boolean conditionChecked
    ) {
        return runMatching(researchNo, searchForm, conditionChecked);
    }

    @PostMapping("/export-xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return ResponseEntity.status(403).body("숨김 처리된 공고에서는 사용할 수 없는 기능입니다.".getBytes(StandardCharsets.UTF_8));
        }
        return exportResponse(legacyMatchingService.prepareMatchingXlsx(researchNo, searchForm.toCondition()));
    }

    @PostMapping("/export-txt")
    public ResponseEntity<byte[]> exportTxt(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") LegacyMatchingSearchForm searchForm
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return ResponseEntity.status(403).body("숨김 처리된 공고에서는 사용할 수 없는 기능입니다.".getBytes(StandardCharsets.UTF_8));
        }
        return exportResponse(legacyMatchingService.prepareMatchingTxt(researchNo, searchForm.toCondition()));
    }

    @GetMapping("/history")
    public String history(
            @PathVariable Long researchNo,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        LegacyMatchingOverview overview = legacyMatchingService.getOverview(researchNo, new LegacyMatchingSearchForm().toCondition());
        LegacyMatchingHistory history = legacyMatchingService.getHistory(researchNo);
        model.addAttribute("pageTitle", "좌담회/설문 회차 실행 이력");
        model.addAttribute("pageDescription", "매칭 결과와 회차 실행 로그를 확인합니다.");
        model.addAttribute("research", overview.research());
        model.addAttribute("history", history);
        model.addAttribute("_csrf", csrfToken);
        return "research/matching-history";
    }

    private String queryParam(String name, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "&" + name + "=" + java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Set<Long> selectedIdSet(List<Long> selectedApplicationIds) {
        return selectedApplicationIds == null ? null : new LinkedHashSet<>(selectedApplicationIds);
    }

    private boolean hasConditionCheckParam(LegacyMatchingSearchForm searchForm) {
        return searchForm != null && searchForm.hasAnyParameter();
    }

    private boolean hasConditionInput(LegacyMatchingSearchForm searchForm) {
        return searchForm != null && searchForm.hasInput();
    }

    private String missingPrerequisiteRedirect(Long researchNo, LegacyMatchingSearchForm searchForm) {
        String reason = hasConditionInput(searchForm) ? "conditionRequired" : "keywordRequired";
        return "redirect:/research/" + researchNo + "/matching?" + reason;
    }

    private String queryParams(LegacyMatchingSearchForm searchForm) {
        if (searchForm == null) {
            return "";
        }
        return queryParam("appSex", searchForm.getAppSex())
                + queryParam("appBirth", searchForm.getAppBirth())
                + queryParam("appJob", searchForm.getAppJob())
                + queryParam("appCompany", searchForm.getAppCompany())
                + queryParam("appAddr", searchForm.getAppAddr())
                + queryParam("addComment", searchForm.getAddComment());
    }

    private String hiddenActionRedirect(Long researchNo) {
        return "redirect:/research/" + researchNo + "?hiddenActionBlocked";
    }

    private ResponseEntity<byte[]> exportResponse(ExportPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(payload.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }

}
