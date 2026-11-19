package com.erp.copilot.service;

import com.erp.copilot.dto.CopilotUserSessionContext;
import com.erp.order.dto.CreateOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Quản lý Trí nhớ Ngữ cảnh Đa lượt (Multi-turn Context Memory & Anaphora Resolution)
 * Lưu trữ in-memory phiên làm việc của từng user với cơ chế tự làm sạch sau 30 phút không hoạt động.
 */
@Slf4j
@Service
public class CopilotContextManager {

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L; // 30 phút
    private final Map<String, CopilotUserSessionContext> sessionMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnaphoraResolution {
        private String resolvedPrompt;
        private boolean hasAnaphora;
        private String targetSku;
        private String targetProductName;
        private String targetCustomerName;
        private String targetCustomerPhone;
        private String targetOrderCode;
    }

    /**
     * Lấy hoặc khởi tạo ngữ cảnh của người dùng
     */
    public CopilotUserSessionContext getContext(String username) {
        String key = (username != null && !username.isBlank()) ? username.trim().toLowerCase() : "anonymous";
        CopilotUserSessionContext ctx = sessionMap.get(key);
        long now = System.currentTimeMillis();

        if (ctx == null || (now - ctx.getLastActiveTimestamp()) > SESSION_TIMEOUT_MS) {
            ctx = CopilotUserSessionContext.builder()
                    .lastActiveTimestamp(now)
                    .build();
            sessionMap.put(key, ctx);
        } else {
            ctx.setLastActiveTimestamp(now);
        }
        return ctx;
    }

    /**
     * Cập nhật thông tin sản phẩm vừa tương tác vào bộ nhớ
     */
    public void updateProduct(String username, Long id, String sku, String name, BigDecimal price, Integer stock) {
        CopilotUserSessionContext ctx = getContext(username);
        ctx.setLastProductId(id);
        ctx.setLastProductSku(sku);
        ctx.setLastProductName(name);
        ctx.setLastProductPrice(price);
        ctx.setLastProductStock(stock);
        ctx.setLastActiveTimestamp(System.currentTimeMillis());
        log.debug("[Context] User '{}' đã cập nhật sản phẩm gần nhất: {} ({})", username, name, sku);
    }

    /**
     * Cập nhật thông tin khách hàng vừa tương tác vào bộ nhớ
     */
    public void updateCustomer(String username, Long id, String name, String phone, BigDecimal debt) {
        CopilotUserSessionContext ctx = getContext(username);
        ctx.setLastCustomerId(id);
        ctx.setLastCustomerName(name);
        ctx.setLastCustomerPhone(phone);
        ctx.setLastCustomerDebt(debt);
        ctx.setLastActiveTimestamp(System.currentTimeMillis());
        log.debug("[Context] User '{}' đã cập nhật khách hàng gần nhất: {} ({})", username, name, phone);
    }

    /**
     * Cập nhật mã đơn hàng vừa thao tác vào bộ nhớ
     */
    public void updateOrder(String username, String orderCode) {
        CopilotUserSessionContext ctx = getContext(username);
        ctx.setLastOrderCode(orderCode);
        ctx.setLastActiveTimestamp(System.currentTimeMillis());
        log.debug("[Context] User '{}' đã cập nhật đơn hàng gần nhất: {}", username, orderCode);
    }

    /**
     * Lưu trữ đơn hàng dự thảo Conversational POS
     */
    public void setDraftOrder(String username, CreateOrderRequest draftOrder) {
        CopilotUserSessionContext ctx = getContext(username);
        ctx.setPendingDraftOrder(draftOrder);
        ctx.setLastActiveTimestamp(System.currentTimeMillis());
    }

    /**
     * Xóa đơn hàng dự thảo sau khi đã tạo đơn thành công hoặc bị hủy
     */
    public void clearDraftOrder(String username) {
        CopilotUserSessionContext ctx = getContext(username);
        ctx.setPendingDraftOrder(null);
    }

    /**
     * Phân giải đại từ thay thế (Anaphora Resolution):
     * "Nó giá bao nhiêu?" -> Ánh xạ về lastProductSku
     * "Ổng nợ bao nhiêu?" -> Ánh xạ về lastCustomerName
     * "Đơn đó tới đâu rồi?" -> Ánh xạ về lastOrderCode
     */
    public AnaphoraResolution resolveAnaphora(String rawPrompt, String unaccented, String username) {
        CopilotUserSessionContext ctx = getContext(username);
        String prompt = rawPrompt != null ? rawPrompt.trim() : "";
        String unacc = unaccented != null ? unaccented.trim() : "";

        boolean hasAnaphora = false;
        String targetSku = null;
        String targetProductName = null;
        String targetCustomerName = null;
        String targetCustomerPhone = null;
        String targetOrderCode = null;
        String resolvedPrompt = prompt;

        // 1. Phân giải Đại từ Sản phẩm: "nó", "mặt hàng này", "sản phẩm này", "cái này", "cái đó", "áo đó"
        boolean refersToProduct = containsWord(unacc, "no") 
                || unacc.contains("san pham nay") 
                || unacc.contains("mat hang nay") 
                || unacc.contains("cai nay") 
                || unacc.contains("cai do")
                || (containsWord(unacc, "gia") && containsWord(unacc, "bao nhieu") && !containsWord(unacc, "tong"))
                || (containsWord(unacc, "ton") && containsWord(unacc, "bao nhieu"));

        if (refersToProduct && ctx.getLastProductSku() != null) {
            hasAnaphora = true;
            targetSku = ctx.getLastProductSku();
            targetProductName = ctx.getLastProductName();
            // Bổ sung tên SKU vào prompt nếu chưa có
            if (!unacc.contains(ctx.getLastProductSku().toLowerCase())) {
                resolvedPrompt = resolvedPrompt + " (SKU: " + ctx.getLastProductSku() + " - " + ctx.getLastProductName() + ")";
            }
        }

        // 2. Phân giải Đại từ Khách hàng: "ổng", "ông ấy", "ông này", "bà ấy", "anh ấy", "chị ấy", "thằng đó", "thằng này", "khách này"
        boolean refersToCustomer = containsWord(unacc, "ong") 
                || containsWord(unacc, "ong ay")
                || containsWord(unacc, "ong nay")
                || containsWord(unacc, "ong do")
                || containsWord(unacc, "ba ay")
                || containsWord(unacc, "anh ay")
                || containsWord(unacc, "chi ay")
                || containsWord(unacc, "thang do")
                || containsWord(unacc, "thang nay")
                || containsWord(unacc, "khach nay")
                || containsWord(unacc, "khach do");

        if (refersToCustomer && ctx.getLastCustomerName() != null) {
            hasAnaphora = true;
            targetCustomerName = ctx.getLastCustomerName();
            targetCustomerPhone = ctx.getLastCustomerPhone();
            if (!unacc.contains(ctx.getLastCustomerName().toLowerCase())) {
                resolvedPrompt = resolvedPrompt + " (Khách hàng: " + ctx.getLastCustomerName() + ")";
            }
        }

        // 3. Phân giải Đại từ Đơn hàng: "đơn đó", "đơn này", "đơn vừa rồi", "đơn vừa tạo"
        boolean refersToOrder = unacc.contains("don do") 
                || unacc.contains("don nay") 
                || unacc.contains("don vua roi") 
                || unacc.contains("don vua tao");

        if (refersToOrder && ctx.getLastOrderCode() != null) {
            hasAnaphora = true;
            targetOrderCode = ctx.getLastOrderCode();
            if (!unacc.contains(ctx.getLastOrderCode().toLowerCase())) {
                resolvedPrompt = resolvedPrompt + " (Mã đơn: " + ctx.getLastOrderCode() + ")";
            }
        }

        return AnaphoraResolution.builder()
                .resolvedPrompt(resolvedPrompt)
                .hasAnaphora(hasAnaphora)
                .targetSku(targetSku)
                .targetProductName(targetProductName)
                .targetCustomerName(targetCustomerName)
                .targetCustomerPhone(targetCustomerPhone)
                .targetOrderCode(targetOrderCode)
                .build();
    }

    private boolean containsWord(String source, String word) {
        if (source == null || word == null) return false;
        String pattern = "(?i)\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern).matcher(source).find();
    }
}
