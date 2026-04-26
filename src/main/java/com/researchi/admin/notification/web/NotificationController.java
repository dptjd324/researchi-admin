package com.researchi.admin.notification.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/notifications/email")
    public String notifyEmail(
            @RequestParam("documentSrl") Long documentSrl,
            @RequestParam("matchJobId") Long matchJobId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        notificationService.sendEmailNotifications(documentSrl, matchJobId, principal, request);
        return "redirect:/matching/jobs/" + documentSrl + "?matchJobId=" + matchJobId + "&emailSent";
    }

    @PostMapping("/notifications/sms")
    public String notifySms(
            @RequestParam("documentSrl") Long documentSrl,
            @RequestParam("matchJobId") Long matchJobId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        notificationService.sendSmsNotifications(documentSrl, matchJobId, principal, request);
        return "redirect:/matching/jobs/" + documentSrl + "?matchJobId=" + matchJobId + "&smsSent";
    }
}
