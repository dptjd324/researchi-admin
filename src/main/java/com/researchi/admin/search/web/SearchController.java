package com.researchi.admin.search.web;

import com.researchi.admin.search.domain.PeriodSearchForm;
import com.researchi.admin.search.domain.PeriodSearchResult;
import com.researchi.admin.search.service.PeriodSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class SearchController {

    private final PeriodSearchService periodSearchService;

    public SearchController(PeriodSearchService periodSearchService) {
        this.periodSearchService = periodSearchService;
    }

    @GetMapping
    public String index(
            @ModelAttribute("searchForm") PeriodSearchForm form,
            @RequestParam(name = "submitted", defaultValue = "false") boolean submitted,
            Model model
    ) {
        String scope = form.getScope() == null ? "APPLICATION" : form.getScope();
        model.addAttribute("pageTitle", "기간 검색");
        model.addAttribute("pageDescription", "등록, 지원, 발송, 알림 이력을 기간과 다중 조건으로 검색합니다.");
        model.addAttribute("scopeOptions", periodSearchService.getScopeOptions());
        model.addAttribute("statusOptions", periodSearchService.getStatusOptions(scope));
        model.addAttribute("jobOptions", periodSearchService.getJobOptions(form.getDocumentSrl()));
        model.addAttribute("submitted", submitted);
        if (submitted) {
            PeriodSearchResult result = periodSearchService.search(form);
            model.addAttribute("result", result);
            model.addAttribute("statusOptions", periodSearchService.getStatusOptions(result.scope()));
        } else {
            model.addAttribute("result", new PeriodSearchResult(scope, "Applied At", 0, null, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        }
        return "search/index";
    }
}
