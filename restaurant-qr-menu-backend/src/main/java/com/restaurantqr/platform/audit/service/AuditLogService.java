package com.restaurantqr.platform.audit.service;

import com.restaurantqr.platform.audit.entity.AuditLog;
import com.restaurantqr.platform.audit.repository.AuditLogRepository;
import com.restaurantqr.platform.security.JwtUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog log(Long restaurantId, String action, String entityType, Long entityId, String oldValue, String newValue) {
        Long userId = null;
        String userName = "SYSTEM";
        String userRole = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails details) {
            userId = details.getUserId();
            userName = details.getEmail();
            userRole = details.getRole();
            if (restaurantId == null) {
                restaurantId = details.getRestaurantId();
            }
        }

        String ipAddress = "127.0.0.1";
        String userAgent = "Unknown";

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            ipAddress = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
            String agentHeader = request.getHeader("User-Agent");
            if (agentHeader != null) {
                userAgent = agentHeader.length() > 500 ? agentHeader.substring(0, 500) : agentHeader;
            }
        }

        AuditLog logEntry = AuditLog.builder()
                .restaurantId(restaurantId)
                .userId(userId)
                .userName(userName)
                .userRole(userRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();

        log.debug("AuditLog recorded: {} on {} (ID: {}) by {}", action, entityType, entityId, userName);
        return auditLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Long restaurantId, Pageable pageable) {
        return auditLogRepository.findByRestaurantIdOrderByTimestampDesc(restaurantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getActivityTimeline(Long restaurantId) {
        return auditLogRepository.findTop20ByRestaurantIdOrderByTimestampDesc(restaurantId);
    }
}
