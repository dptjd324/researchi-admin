package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.legacy.matching.domain.LegacyKeywordIndexResult;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingOverview;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingResult;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@Profile("local")
public class LocalMatchingConsentPreviewController {

    private static final long PREVIEW_RESEARCH_NO = -99001L;

    @GetMapping("/dev/matching-consent-preview")
    public String preview(Model model, CsrfToken csrfToken) {
        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(PREVIEW_RESEARCH_NO);
        research.setResearchTitle("채널별 동의 매칭 결과 미리보기");

        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                "남자, 여자",
                "1980-2000",
                null,
                null,
                "서울",
                "금융"
        );
        LegacyMatchingSearchForm searchForm = new LegacyMatchingSearchForm();
        searchForm.setAppSex("남자, 여자");
        searchForm.setAppBirth("1980-2000");
        searchForm.setAppAddr("서울");
        searchForm.setAddComment("금융");

        List<LegacyMatchingResult> results = List.of(
                previewResult(1, -990011L, "문자동의", "1", "39", "010-0000-1001", "sms-preview@example.com", true, false),
                previewResult(2, -990012L, "메일동의", "2", "36", "010-0000-1002", "email-preview@example.com", false, true),
                previewResult(3, -990013L, "전체동의", "1", "42", "010-0000-1003", "all-preview@example.com", true, true)
        );

        LegacyMatchingOverview overview = new LegacyMatchingOverview(
                research,
                condition.storageKey(),
                "",
                condition,
                3,
                condition.displayFilters(),
                results,
                3,
                3,
                0,
                1,
                2,
                500,
                null,
                "COMPLETED",
                null,
                LocalDateTime.now()
        );

        model.addAttribute("pageTitle", "채널별 동의 매칭 미리보기");
        model.addAttribute("research", research);
        model.addAttribute("overview", overview);
        model.addAttribute("searchForm", searchForm);
        model.addAttribute("result", new LegacyKeywordIndexResult(3, 3).withCycleNo(1));
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute("previewMode", true);
        return "research/matching-run-window";
    }

    private LegacyMatchingResult previewResult(
            int rowNo,
            Long researchAppSeq,
            String name,
            String sex,
            String age,
            String phone,
            String email,
            boolean smsAllowed,
            boolean emailAllowed
    ) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(PREVIEW_RESEARCH_NO);
        application.setResearchAppSeq(researchAppSeq);
        application.setAppName(name);
        application.setAppSex(sex);
        application.setAppBirth("900101");
        application.setAppAge(age);
        application.setAppJob("회사원");
        application.setAppCompany("가상 리서치 회사");
        application.setAppHphone(phone);
        application.setAppEmail(email);
        application.setAppAddr("서울시 가상구");
        application.setAddComment("금융 서비스 이용 경험 / 가상 미리보기 데이터");
        return new LegacyMatchingResult(
                rowNo,
                application,
                3,
                List.of("성별", "생년월일", "추가기재사항: 금융"),
                List.of()
        ).withConsentStatus(smsAllowed, emailAllowed);
    }
}
