package com.erp.audit;

import com.erp.workflow.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor tự động ghi nhận lịch sử thao tác (Audit Trail) cho mọi API thay đổi dữ liệu (POST/PUT/PATCH/DELETE).
 * Giúp bao phủ 100% các phân hệ mà không làm thay đổi hay ảnh hưởng logic nghiệp vụ hiện hữu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityAuditInterceptor implements HandlerInterceptor {

    public static final String ATTR_AUDIT_LOGGED = "AUDIT_LOGGED";

    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern ID_IN_PATH_PATTERN = Pattern.compile("/(\\d+)(?:/|$)");

    private final AuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            String method = request.getMethod();
            if (method == null || !MUTATION_METHODS.contains(method.toUpperCase())) {
                return;
            }

            if (Boolean.TRUE.equals(request.getAttribute(ATTR_AUDIT_LOGGED))) {
                return;
            }

            String uri = request.getRequestURI();
            if (uri == null || !uri.startsWith("/api/") || shouldSkipUri(uri)) {
                return;
            }

            int status = response.getStatus();
            if (status < 200 || status >= 400) {
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                    ? auth.getName()
                    : "system";
            String role = extractPrimaryRole(auth);

            String action = resolveAction(method, uri);
            String entityType = resolveEntityType(uri);
            Long entityId = extractEntityId(uri);
            String reason = buildReadableReason(action, entityType, uri, status);
            String ipAddress = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 250) {
                userAgent = userAgent.substring(0, 250);
            }

            auditLogService.logActionSafe(
                    username,
                    role,
                    action,
                    entityType,
                    entityId,
                    null,
                    null,
                    null,
                    reason,
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            log.debug("[AUDIT-INTERCEPTOR] Bỏ qua lỗi ghi log: {}", e.getMessage());
        }
    }

    private boolean shouldSkipUri(String uri) {
        return uri.startsWith("/api/auth/login")
                || uri.startsWith("/api/copilot/stream")
                || uri.startsWith("/api/copilot/chat")
                || uri.startsWith("/api/copilot/ask")
                || uri.startsWith("/api/chat/")
                || uri.startsWith("/api/workflow/approvals/")
                || uri.startsWith("/api/audit-logs");
    }

    private String resolveAction(String method, String uri) {
        String lowerUri = uri.toLowerCase();
        if (lowerUri.contains("/cancel")) return "CANCEL";
        if (lowerUri.contains("/approve")) return "APPROVE";
        if (lowerUri.contains("/reject")) return "REJECT";
        if (lowerUri.contains("/lock")) return "LOCK";
        if (lowerUri.contains("/unlock")) return "UNLOCK";
        if (lowerUri.contains("/password")) return "CHANGE_PASSWORD";
        return switch (method.toUpperCase()) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method.toUpperCase();
        };
    }

    private String resolveEntityType(String uri) {
        if (uri.startsWith("/api/products")) return "PRODUCT";
        if (uri.startsWith("/api/orders")) return "ORDER";
        if (uri.startsWith("/api/users")) return "USER";
        if (uri.startsWith("/api/auth")) return "AUTH";
        if (uri.startsWith("/api/finance") || uri.startsWith("/api/treasury") || uri.startsWith("/api/bank-reconciliation")) {
            return "FINANCE";
        }
        if (uri.startsWith("/api/procurement")) return "PROCUREMENT";
        if (uri.startsWith("/api/promotions")) return "PROMOTION";
        if (uri.startsWith("/api/branches") || uri.startsWith("/api/transfers")) return "BRANCH_WAREHOUSE";
        if (uri.startsWith("/api/inventory")) return "INVENTORY";
        if (uri.startsWith("/api/announcements")) return "ANNOUNCEMENT";
        if (uri.startsWith("/api/calendar")) return "CALENDAR";
        if (uri.startsWith("/api/scheduler")) return "JOB_SCHEDULER";
        if (uri.startsWith("/api/copilot")) return "AI_COPILOT";
        if (uri.startsWith("/api/workflow")) return "WORKFLOW";
        return "SYSTEM";
    }

    private Long extractEntityId(String uri) {
        Matcher matcher = ID_IN_PATH_PATTERN.matcher(uri);
        Long lastId = null;
        while (matcher.find()) {
            try {
                lastId = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return lastId;
    }

    private String buildReadableReason(String action, String entityType, String uri, int status) {
        String actionLabel = switch (action) {
            case "CREATE" -> "Thêm mới";
            case "UPDATE" -> "Cập nhật";
            case "DELETE" -> "Xóa dữ liệu";
            case "CANCEL" -> "Hủy chứng từ";
            case "APPROVE" -> "Phê duyệt";
            case "REJECT" -> "Từ chối";
            case "LOCK" -> "Khóa tài khoản";
            case "UNLOCK" -> "Mở khóa tài khoản";
            case "CHANGE_PASSWORD" -> "Đổi mật khẩu";
            default -> action;
        };
        return actionLabel + " tại phân hệ " + entityType + " (" + uri + ") [HTTP " + status + "]";
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractPrimaryRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return "SYSTEM";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
    }
}
