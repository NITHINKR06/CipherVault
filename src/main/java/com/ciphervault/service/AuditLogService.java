package com.ciphervault.service;

import com.ciphervault.model.AuditLog;
import com.ciphervault.model.User;
import com.ciphervault.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired private AuditLogRepository auditLogRepository;

    public void log(User user, String action, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
            .user(user)
            .action(action)
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : "N/A")
            .build();
        auditLogRepository.save(log);
    }

    public void log(User user, String action) {
        log(user, action, null);
    }

    public List<AuditLog> getRecentLogs(User user) {
        return auditLogRepository.findTop20ByUserOrderByTimestampDesc(user);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
