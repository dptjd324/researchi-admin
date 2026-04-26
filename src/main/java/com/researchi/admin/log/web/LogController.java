package com.researchi.admin.log.web;

import com.researchi.admin.log.service.AdminLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logs")
public class LogController {

    private final AdminLogService adminLogService;

    public LogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @GetMapping("/actions")
    public String actions(Model model) {
        model.addAttribute("pageTitle", "\uC561\uC158 \uB85C\uADF8");
        model.addAttribute("pageDescription", "\uAD00\uB9AC\uC790 \uC791\uC5C5 \uC774\uB825\uC744 \uC561\uC158 \uC720\uD615, \uB300\uC0C1, \uC0DD\uC131 \uC2DC\uAC01 \uAE30\uC900\uC73C\uB85C \uD655\uC778\uD569\uB2C8\uB2E4.");
        model.addAttribute("actionLogs", adminLogService.getActionLogs());
        return "logs/actions";
    }

    @GetMapping("/mail")
    public String mail(Model model) {
        model.addAttribute("pageTitle", "\uBA54\uC77C \uB85C\uADF8");
        model.addAttribute("pageDescription", "\uBC1C\uC1A1 \uD654\uBA74\uC5D0\uC11C \uBC1C\uC0DD\uD55C \uC218\uB3D9 \uBC0F \uC790\uB3D9 \uBA54\uC77C \uBC1C\uC1A1 \uC774\uB825\uC744 \uD655\uC778\uD569\uB2C8\uB2E4.");
        model.addAttribute("mailLogs", adminLogService.getMailLogs());
        return "logs/mail";
    }

    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("pageTitle", "\uAC80\uC0C9 \uB85C\uADF8");
        model.addAttribute("pageDescription", "\uC800\uC7A5\uB41C \uAE30\uAC04 \uAC80\uC0C9 \uC870\uAC74\uACFC \uAC80\uC0C9 \uACB0\uACFC \uAC74\uC218\uB97C \uD655\uC778\uD569\uB2C8\uB2E4.");
        model.addAttribute("searchLogs", adminLogService.getSearchLogs());
        return "logs/search";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("pageTitle", "\uC54C\uB9BC \uB85C\uADF8");
        model.addAttribute("pageDescription", "\uC774\uBA54\uC77C\uACFC SMS \uC9C0\uC6D0\uC790 \uC54C\uB9BC \uBC1C\uC1A1 \uACB0\uACFC\uB97C \uD655\uC778\uD569\uB2C8\uB2E4.");
        model.addAttribute("notificationLogs", adminLogService.getNotificationLogs());
        return "logs/notifications";
    }
}
