package com.researchi.admin.log.web;

import com.researchi.admin.common.web.PaginationSupport;
import com.researchi.admin.log.service.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logs")
public class LogController {

    private static final int LOG_PAGE_SIZE = 20;

    private final AdminLogService adminLogService;

    public LogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @GetMapping("/actions")
    public String actions(
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("pageTitle", "액션 로그");
        model.addAttribute("pageDescription", "관리자 작업 이력을 액션 유형, 대상, 생성 시각 기준으로 확인합니다.");
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                cappedCount(adminLogService.countActionLogs()),
                page,
                LOG_PAGE_SIZE
        );
        model.addAttribute("actionLogs", adminLogService.getActionLogsPage(pageWindow.pageSize(), pageWindow.offset()));
        return "logs/actions";
    }

    @GetMapping("/mail")
    public String mail(
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("pageTitle", "메일 로그");
        model.addAttribute("pageDescription", "발송 화면에서 발생한 수동 및 자동 메일 발송 이력을 확인합니다.");
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                cappedCount(adminLogService.countMailLogs()),
                page,
                LOG_PAGE_SIZE
        );
        model.addAttribute("mailLogs", adminLogService.getMailLogsPage(pageWindow.pageSize(), pageWindow.offset()));
        return "logs/mail";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("pageTitle", "검색 로그");
        model.addAttribute("pageDescription", "저장된 기간 검색 조건과 검색 결과 건수를 확인합니다.");
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                cappedCount(adminLogService.countSearchLogs()),
                page,
                LOG_PAGE_SIZE
        );
        model.addAttribute("searchLogs", adminLogService.getSearchLogsPage(pageWindow.pageSize(), pageWindow.offset()));
        return "logs/search";
    }

    @GetMapping("/notifications")
    public String notifications(
            @RequestParam(name = "page", required = false) Integer page,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("pageTitle", "알림 로그");
        model.addAttribute("pageDescription", "이메일과 SMS 지원자 알림 발송 결과를 확인합니다.");
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                cappedCount(adminLogService.countNotificationLogs()),
                page,
                LOG_PAGE_SIZE
        );
        model.addAttribute("notificationLogs", adminLogService.getNotificationLogsPage(pageWindow.pageSize(), pageWindow.offset()));
        return "logs/notifications";
    }

    private int cappedCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, count);
    }
}
