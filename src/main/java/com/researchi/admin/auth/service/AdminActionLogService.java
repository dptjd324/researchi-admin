package com.researchi.admin.auth.service;

import com.researchi.admin.auth.domain.AdminActionLog;
import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActionLogService {

    private final AdminActionLogMapper adminActionLogMapper;

    public AdminActionLogService(AdminActionLogMapper adminActionLogMapper) {
        this.adminActionLogMapper = adminActionLogMapper;
    }

    @Transactional("adminTransactionManager")
    public void log(
            Long adminUserId,
            String actionType,
            String targetType,
            String targetId,
            String actionDetail,
            HttpServletRequest request
    ) {
        AdminActionLog actionLog = new AdminActionLog();
        actionLog.setAdminUserId(adminUserId);
        actionLog.setActionType(actionType);
        actionLog.setTargetType(targetType);
        actionLog.setTargetId(targetId);
        actionLog.setActionDetail(actionDetail);
        actionLog.setIpAddress(resolveIp(request));
        adminActionLogMapper.insert(actionLog);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "SYSTEM";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex > -1 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }
}
