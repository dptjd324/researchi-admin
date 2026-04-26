package com.researchi.admin.web.support;

import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.log.service.AdminLogService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminStatusBarAdvice {

    private final AdminLogService adminLogService;

    public AdminStatusBarAdvice(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @ModelAttribute("statusBarSummary")
    public StatusBarSummary statusBarSummary() {
        return adminLogService.getStatusBarSummary();
    }
}
