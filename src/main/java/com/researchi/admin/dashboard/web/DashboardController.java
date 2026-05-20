package com.researchi.admin.dashboard.web;

import com.researchi.admin.dashboard.domain.DashboardMessageUsage;
import com.researchi.admin.dashboard.service.DashboardUsageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardUsageService dashboardUsageService;

    public DashboardController(DashboardUsageService dashboardUsageService) {
        this.dashboardUsageService = dashboardUsageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardMessageUsage messageUsage = dashboardUsageService.getMessageUsage();
        model.addAttribute("pageTitle", "대시보드");
        model.addAttribute("pageDescription", "월별 메일과 SMS 발송량을 확인합니다.");
        model.addAttribute("messageUsage", messageUsage);
        model.addAttribute("maxMonthlyTotal", Math.max(messageUsage.maxMonthlyTotal(), 1));
        return "home/index";
    }
}
