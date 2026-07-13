package com.researchi.admin.legacy.research.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.client.service.ResearchClientLinkService;
import com.researchi.admin.common.web.PaginationSupport;
import com.researchi.admin.legacy.blacklist.service.LegacyBlacklistService;
import com.researchi.admin.legacy.blacklist.web.LegacyBlacklistForm;
import com.researchi.admin.legacy.publish.service.ManualPublishService;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchApplicationDuplicateGroup;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchApplicationService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.web.support.TextLinkRenderer;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/research")
public class ResearchMasterController {

    private static final int RESEARCH_LIST_PAGE_SIZE = 50;
    private static final int ALL_APPLICATION_SEARCH_PAGE_SIZE = 100;
    private final ResearchMasterService researchMasterService;
    private final ResearchApplicationService researchApplicationService;
    private final ManualPublishService manualPublishService;
    private final NotificationProperties notificationProperties;
    private final TextLinkRenderer textLinkRenderer;
    private final ClientService clientService;
    private final ResearchClientLinkService researchClientLinkService;
    private final LegacyBlacklistService legacyBlacklistService;

    public ResearchMasterController(
            ResearchMasterService researchMasterService,
            ResearchApplicationService researchApplicationService,
            ManualPublishService manualPublishService,
            NotificationProperties notificationProperties,
            TextLinkRenderer textLinkRenderer,
            ClientService clientService,
            ResearchClientLinkService researchClientLinkService,
            LegacyBlacklistService legacyBlacklistService
    ) {
        this.researchMasterService = researchMasterService;
        this.researchApplicationService = researchApplicationService;
        this.manualPublishService = manualPublishService;
        this.notificationProperties = notificationProperties;
        this.textLinkRenderer = textLinkRenderer;
        this.clientService = clientService;
        this.researchClientLinkService = researchClientLinkService;
        this.legacyBlacklistService = legacyBlacklistService;
    }

    @GetMapping
    public String list(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "registStart", required = false) String registStart,
            @RequestParam(name = "registEnd", required = false) String registEnd,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "serverName", required = false) String serverName,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        return listView(model, keyword, registStart, registEnd, title, companyName, serverName, sort, direction, page, request, csrfToken, false);
    }

    @GetMapping("/hidden")
    public String hiddenList(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "registStart", required = false) String registStart,
            @RequestParam(name = "registEnd", required = false) String registEnd,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "serverName", required = false) String serverName,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        return listView(model, keyword, registStart, registEnd, title, companyName, serverName, sort, direction, page, request, csrfToken, true);
    }

    private String listView(
            Model model,
            String keyword,
            String registStart,
            String registEnd,
            String title,
            String companyName,
            String serverName,
            String sort,
            String direction,
            Integer page,
            HttpServletRequest request,
            CsrfToken csrfToken,
            boolean hiddenOnly
    ) {
        request.getSession(true);
        model.addAttribute("pageTitle", "좌담회/설문");
        model.addAttribute("pageDescription", "좌담회/설문 공고를 조회합니다.");

        String normalizedSort = normalizeListSort(sort);
        String normalizedDirection = normalizeListDirection(normalizedSort, direction);

        int totalCount = researchMasterService.countResearchMasters(
                keyword,
                registStart,
                registEnd,
                title,
                companyName,
                serverName,
                hiddenOnly,
                normalizedSort
        );
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                totalCount,
                page,
                RESEARCH_LIST_PAGE_SIZE
        );
        List<ResearchMaster> researchMasters = researchMasterService.getResearchMasterPage(
                keyword,
                registStart,
                registEnd,
                title,
                companyName,
                serverName,
                hiddenOnly,
                normalizedSort,
                normalizedDirection,
                pageWindow.pageSize(),
                pageWindow.offset()
        );

        model.addAttribute("researchMasters", researchMasters);
        model.addAttribute("keyword", keyword);
        model.addAttribute("registStart", registStart);
        model.addAttribute("registEnd", registEnd);
        model.addAttribute("title", title);
        model.addAttribute("companyName", companyName);
        model.addAttribute("serverName", serverName);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("direction", normalizedDirection);
        model.addAttribute("nextCloseDateSort", nextDateSort(normalizedSort, normalizedDirection, "closeDate"));
        model.addAttribute("nextCloseDateSortDirection", nextDateSortDirection(normalizedSort, normalizedDirection, "closeDate"));
        model.addAttribute("nextRegistDtSort", nextDateSort(normalizedSort, normalizedDirection, "registDt"));
        model.addAttribute("nextRegistDtSortDirection", nextDateSortDirection(normalizedSort, normalizedDirection, "registDt"));
        String nextApplicantSort = nextApplicantSort(normalizedSort);
        model.addAttribute("nextApplicantSort", nextApplicantSort);
        model.addAttribute("nextApplicantSortDirection", nextApplicantSort == null ? null : "desc");
        model.addAttribute("hiddenMode", hiddenOnly);
        model.addAttribute("_csrf", csrfToken);
        return "research/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, CsrfToken csrfToken) {
        model.addAttribute("pageTitle", "좌담회/설문 등록");
        model.addAttribute("pageDescription", "새 좌담회/설문을 등록합니다.");
        model.addAttribute("researchForm", ResearchMasterForm.from(null));
        model.addAttribute("clientOptions", clientService.getClientSummaries());
        model.addAttribute("_csrf", csrfToken);
        return "research/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("researchForm") ResearchMasterForm form,
            BindingResult bindingResult,
            Model model,
            CsrfToken csrfToken
    ) {
        validateCloseDate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "좌담회/설문 등록");
            model.addAttribute("pageDescription", "새 좌담회/설문을 등록합니다.");
            model.addAttribute("clientOptions", clientService.getClientSummaries());
            model.addAttribute("_csrf", csrfToken);
            return "research/form";
        }
        Long researchNo = researchMasterService.createResearchMaster(form.toResearchMaster(null));
        researchClientLinkService.saveLink(researchNo, form.getClientId());
        return "redirect:/research/" + researchNo + "?created";
    }

    @GetMapping("/{researchNo}")
    public String detail(
            @PathVariable Long researchNo,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        applyCurrentApplicantCounts(researchMaster);

        model.addAttribute("pageTitle", "좌담회/설문 상세");
        model.addAttribute("pageDescription", "좌담회/설문 상세 정보를 확인합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("hiddenResearch", researchMasterService.isHidden(researchNo));
        ResearchMasterForm form = ResearchMasterForm.from(researchMaster);
        form.setClientId(researchClientLinkService.getClientId(researchNo));
        model.addAttribute("researchForm", form);
        model.addAttribute("publicApplyUrl", buildPublicApplyUrl(researchNo));
        model.addAttribute("researchContentsHtml", textLinkRenderer.renderWithDefaultApplyButton(researchMaster.getResearchContents(), buildPublicApplyUrl(researchNo)));
        model.addAttribute("addCommentHtml", textLinkRenderer.render(researchMaster.getAddComment()));
        model.addAttribute("clientOptions", clientService.getClientSummaries());
        model.addAttribute("_csrf", csrfToken);
        return "research/detail";
    }

    @PostMapping("/{researchNo}/hide")
    public String hide(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        researchMasterService.hideResearchMaster(researchNo, principal == null ? null : principal.getId());
        return "redirect:/research?hidden";
    }

    @PostMapping("/{researchNo}/restore")
    public String restore(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        researchMasterService.restoreResearchMaster(researchNo, principal == null ? null : principal.getId());
        return "redirect:/research/hidden?restored";
    }

    @PostMapping("/{researchNo}")
    public String update(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("researchForm") ResearchMasterForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster current = researchMasterService.getResearchMaster(researchNo);
        applyCurrentApplicantCounts(current);
        validateCloseDate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "좌담회/설문 상세");
            model.addAttribute("pageDescription", "좌담회/설문 정보를 수정합니다.");
            model.addAttribute("research", current);
            model.addAttribute("hiddenResearch", researchMasterService.isHidden(researchNo));
            form.setClientId(researchClientLinkService.getClientId(researchNo));
            model.addAttribute("publicApplyUrl", buildPublicApplyUrl(researchNo));
            model.addAttribute("researchContentsHtml", textLinkRenderer.renderWithDefaultApplyButton(form.getResearchContents(), buildPublicApplyUrl(researchNo)));
            model.addAttribute("addCommentHtml", textLinkRenderer.render(form.getAddComment()));
            model.addAttribute("clientOptions", clientService.getClientSummaries());
            model.addAttribute("_csrf", csrfToken);
            return "research/detail";
        }

        researchMasterService.updateResearchMaster(form.toResearchMaster(researchNo), principal == null ? null : principal.getId());
        researchClientLinkService.saveLink(researchNo, form.getClientId());
        return "redirect:/research/" + researchNo + "?updated";
    }

    @GetMapping("/{researchNo}/applications")
    public String applications(
            @PathVariable Long researchNo,
            @ModelAttribute("searchForm") ResearchApplicationSearchForm searchForm,
            @RequestParam(name = "page", required = false) Integer page,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        applyCurrentApplicantCounts(researchMaster);
        boolean allResearchSearch = searchForm.isAllResearch();
        boolean canQueryApplications = !allResearchSearch || hasMinimumLengthSearchCondition(searchForm);
        int totalCount = canQueryApplications ? researchApplicationService.countApplications(researchNo, searchForm) : 0;

        model.addAttribute("pageTitle", "좌담회/설문 지원자");
        model.addAttribute("pageDescription", "지원자 정보를 조회합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("researchCodeText", "CODE " + researchMaster.getResearchNo());
        model.addAttribute(
                "researchApplicantCountText",
                " | applicants " + nullToZero(researchMaster.getAppNewCnt()) + "/" + nullToZero(researchMaster.getAppCnt())
        );
        List<ResearchApplication> applications = List.of();
        int applicantRowOffset = 0;
        if (canQueryApplications && allResearchSearch) {
            PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                    model,
                    request,
                    totalCount,
                    page,
                    ALL_APPLICATION_SEARCH_PAGE_SIZE
            );
            applications = researchApplicationService.getApplicationPage(
                    researchNo,
                    searchForm,
                    pageWindow.pageSize(),
                    pageWindow.offset()
            );
            applicantRowOffset = pageWindow.offset();
        } else if (canQueryApplications) {
            applications = researchApplicationService.getApplications(researchNo, searchForm);
            model.addAttribute("paginationEnabled", false);
        } else {
            model.addAttribute("paginationEnabled", false);
            model.addAttribute("totalItemCount", 0);
        }
        int uniqueApplicantCount = countUniqueApplicants(applications);
        int duplicateApplicantCount = Math.max(0, applications.size() - uniqueApplicantCount);
        model.addAttribute("applications", applications);
        model.addAttribute("totalItemCount", totalCount);
        model.addAttribute("applicantRowOffset", applicantRowOffset);
        model.addAttribute("uniqueApplicantCount", uniqueApplicantCount);
        model.addAttribute("duplicateApplicantCount", duplicateApplicantCount);
        model.addAttribute("allResearchSearch", allResearchSearch);
        model.addAttribute("canQueryApplications", canQueryApplications);
        model.addAttribute("minimumSearchLength", 2);
        model.addAttribute(
                "researchApplicantSummaryText",
                nullToZero(researchMaster.getAppNewCnt()) + "/" + nullToZero(researchMaster.getAppCnt()) + " 공고 표시 신청자"
        );
        model.addAttribute(
                "duplicateApplicantNotice",
                duplicateApplicantCount > 0
                        ? "기존 DB에 동일 신청자로 보이는 행이 " + duplicateApplicantCount + "건 있어, 조회 행 수와 중복 제외 추정 인원을 함께 표시합니다."
                        : "새 신청 폼은 중복 신청을 방지하므로, 이 안내는 기존 DB 이전 데이터 확인용입니다."
        );
        model.addAttribute("viewMode", "PROVIDE".equals(searchForm.getViewMode()) ? "PROVIDE" : "ALL");
        model.addAttribute("_csrf", csrfToken);
        return "research/applications";
    }

    private boolean hasMinimumLengthSearchCondition(ResearchApplicationSearchForm searchForm) {
        if (searchForm == null) {
            return false;
        }
        return hasMinimumLengthText(searchForm.getAppName())
                || hasMinimumLengthText(searchForm.getAppBirth())
                || hasMinimumLengthText(searchForm.getAppAge())
                || hasMinimumLengthText(searchForm.getAppJob())
                || hasMinimumLengthText(searchForm.getAppCompany())
                || hasMinimumLengthText(searchForm.getAppHphone())
                || hasMinimumLengthText(searchForm.getAppTele())
                || hasMinimumLengthText(searchForm.getAppEmail())
                || hasMinimumLengthText(searchForm.getAppAddr())
                || hasMinimumLengthText(searchForm.getAddComment())
                || hasMinimumLengthText(searchForm.getAttendResearch());
    }

    private boolean hasMinimumLengthText(String value) {
        return value != null && value.trim().length() >= 2;
    }

    @GetMapping("/{researchNo}/applications/provide-preview")
    public String providePreview(
            @PathVariable Long researchNo,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        applyCurrentApplicantCounts(researchMaster);
        List<ResearchApplication> applications = researchApplicationService.getUnprovidedApplications(researchNo);

        model.addAttribute("pageTitle", "제공 대상 미리보기");
        model.addAttribute("research", researchMaster);
        model.addAttribute("applications", applications);
        model.addAttribute("totalItemCount", applications.size());
        model.addAttribute("_csrf", csrfToken);
        return "research/provide-preview";
    }

    @GetMapping("/{researchNo}/applications/duplicates")
    public String duplicateApplications(
            @PathVariable Long researchNo,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        applyCurrentApplicantCounts(researchMaster);
        List<ResearchApplicationDuplicateGroup> duplicateGroups = researchApplicationService.getDuplicateGroups(researchNo);

        model.addAttribute("pageTitle", "중복 신청자 진단");
        model.addAttribute("pageDescription", "DB의 중복 신청자 키를 삭제 없이 확인합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("duplicateGroups", duplicateGroups);
        model.addAttribute("totalItemCount", duplicateGroups.size());
        model.addAttribute("_csrf", csrfToken);
        return "research/application-duplicates";
    }

    @PostMapping("/{researchNo}/applications/provide-complete")
    public String completeProvision(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        int updatedCount = researchApplicationService.completeProvisionForUnprovided(
                researchNo,
                principal == null ? null : principal.getId()
        );
        return "redirect:/research/" + researchNo + "/applications/provide-preview?provided=" + updatedCount;
    }

    @GetMapping("/{researchNo}/applications/{researchAppSeq}")
    public String applicationDetail(
            @PathVariable Long researchNo,
            @PathVariable Long researchAppSeq,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        ResearchApplication application = researchApplicationService.getApplication(researchNo, researchAppSeq);

        model.addAttribute("pageTitle", "좌담회/설문 지원자 상세");
        model.addAttribute("pageDescription", "DB의 지원자 데이터를 확인합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("applicant", application);
        model.addAttribute("applicantResearchHistory", researchApplicationService.getApplicantResearchHistory(application));
        model.addAttribute(
                "formattedAdditionalAnswers",
                researchApplicationService.getFormattedAdditionalAnswers(researchNo, researchAppSeq, researchMaster)
        );
        model.addAttribute("_csrf", csrfToken);
        return "research/application-detail";
    }

    @GetMapping("/{researchNo}/publish-copy")
    public String publishCopy(
            @PathVariable Long researchNo,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);

        model.addAttribute("pageTitle", "홈페이지 게시문");
        model.addAttribute("pageDescription", "제목과 본문을 수동 복사용으로 생성합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("publishContent", manualPublishService.generateContent(researchMaster));
        model.addAttribute("latestPublishLog", manualPublishService.getLatestLog(researchNo));
        model.addAttribute("_csrf", csrfToken);
        return "research/publish-copy";
    }

    @PostMapping("/{researchNo}/manual-publish-log")
    public String recordManualPublish(
            @PathVariable Long researchNo,
            @RequestParam(name = "publicDocumentSrl", required = false) String publicDocumentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        manualPublishService.recordPublished(
                researchMaster,
                parseLongOrNull(publicDocumentSrl),
                principal == null ? null : principal.getId()
        );
        return "redirect:/research/" + researchNo + "/publish-copy?published";
    }

    @PostMapping("/{researchNo}/applications/{researchAppSeq}/provide")
    public String updateApplicationProvideYn(
            @PathVariable Long researchNo,
            @PathVariable Long researchAppSeq,
            @RequestParam("provideYn") String provideYn,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        researchApplicationService.updateProvideYn(
                researchNo,
                researchAppSeq,
                provideYn,
                principal == null ? null : principal.getId()
        );
        return "redirect:/research/" + researchNo + "/applications/" + researchAppSeq + "?provideUpdated";
    }

    @PostMapping("/{researchNo}/applications/{researchAppSeq}/blacklist")
    public String registerApplicationBlacklist(
            @PathVariable Long researchNo,
            @PathVariable Long researchAppSeq,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        request.getSession(true);
        ResearchApplication application = researchApplicationService.getApplication(researchNo, researchAppSeq);
        if (application.isBlacklisted()) {
            return "redirect:/research/" + researchNo + "/applications?alreadyBlacklisted";
        }

        Long changedBy = principal == null ? null : principal.getId();
        Long blacklistNo = legacyBlacklistService.save(LegacyBlacklistForm.fromApplicant(application), changedBy);
        researchApplicationService.updateProvideYn(researchNo, researchAppSeq, "Y", changedBy);
        return "redirect:/research/" + researchNo + "/applications?blacklisted=" + blacklistNo;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeListSort(String sort) {
        if ("appNewCnt".equals(sort) || "closeDate".equals(sort) || "registDt".equals(sort)) {
            return sort;
        }
        return null;
    }

    private String normalizeListDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    private String normalizeListDirection(String sort, String direction) {
        return "appNewCnt".equals(sort) ? "desc" : normalizeListDirection(direction);
    }

    private String nextDateSort(String currentSort, String currentDirection, String targetSort) {
        if (!targetSort.equals(currentSort)) {
            return targetSort;
        }
        return "asc".equals(currentDirection) ? null : targetSort;
    }

    private String nextDateSortDirection(String currentSort, String currentDirection, String targetSort) {
        if (!targetSort.equals(currentSort)) {
            return "desc";
        }
        return "desc".equals(currentDirection) ? "asc" : null;
    }

    private String nextApplicantSort(String currentSort) {
        return "appNewCnt".equals(currentSort) ? null : "appNewCnt";
    }

    private void applyCurrentApplicantCounts(List<ResearchMaster> researchMasters) {
        if (researchMasters == null || researchMasters.isEmpty()) {
            return;
        }
        for (ResearchMaster researchMaster : researchMasters) {
            applyCurrentApplicantCounts(researchMaster);
        }
    }

    private void applyCurrentApplicantCounts(ResearchMaster researchMaster) {
        if (researchMaster == null || researchMaster.getResearchNo() == null) {
            return;
        }
        researchMaster.setAppCnt(researchApplicationService.countAllApplications(researchMaster.getResearchNo()));
        researchMaster.setAppNewCnt(researchApplicationService.countUnprovidedApplications(researchMaster.getResearchNo()));
    }

    private int countUniqueApplicants(List<ResearchApplication> applications) {
        Set<String> applicantKeys = new LinkedHashSet<>();
        for (ResearchApplication application : applications) {
            applicantKeys.add(buildApplicantKey(application));
        }
        return applicantKeys.size();
    }

    private String buildApplicantKey(ResearchApplication application) {
        String phone = digitsOnly(firstNonBlank(application.getAppHphone(), application.getAppTele()));
        if (phone != null) {
            return "phone:" + phone;
        }
        String name = trimToNull(application.getAppName());
        String birth = digitsOnly(application.getAppBirth());
        if (name != null && birth != null) {
            return "name-birth:" + name + ":" + birth;
        }
        return "seq:" + application.getResearchAppSeq();
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        return normalizedFirst == null ? trimToNull(second) : normalizedFirst;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private void validateCloseDate(ResearchMasterForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("closeDate")) {
            return;
        }
        String digits = digitsOnly(form.getCloseDate());
        if (digits == null || digits.length() < 8) {
            bindingResult.rejectValue("closeDate", "required", "마감일자를 8자리 날짜로 입력해 주세요.");
        }
    }

    private String buildPublicApplyUrl(Long researchNo) {
        String baseUrl = notificationProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8082";
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/research/" + researchNo + "/apply";
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value.trim());
    }
}
