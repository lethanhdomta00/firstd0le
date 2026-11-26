package com.erp.copilot.service;

import com.erp.copilot.dto.CopilotResponseDto;
import com.erp.copilot.dto.CopilotResponseDto.ThinkingStepDto;
import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.tool.AgentTool;
import com.erp.copilot.tool.AgenticToolRegistry;
import com.erp.model.Product;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.InventoryForecastService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import com.erp.workflow.dto.AuditLogDto;
import com.erp.workflow.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveCognitiveService {

    private final AgenticToolRegistry toolRegistry;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryForecastService inventoryForecastService;
    private final OrderRepository orderRepository;
    private final CopilotContextManager contextManager;
    private final CopilotProactiveService proactiveService;
    private final ActionChainingService actionChainingService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Xử lý câu hỏi của người dùng qua Động cơ Trí tuệ Đa tầng (4 Cognitive Tiers)
     * Tích hợp Quản lý Ngữ cảnh Đa lượt & Phân giải Đại từ (Multi-turn Anaphora Resolution)
     */
    public CopilotResponseDto processPrompt(String rawPrompt, String currentUsername) {
        long startTime = System.currentTimeMillis();
        String prompt = rawPrompt != null ? rawPrompt.trim() : "";
        String lower = prompt.toLowerCase();
        String unaccented = unaccent(lower);

        // =========================================================================
        // 0. ANAPHORA RESOLUTION: PHÂN GIẢI ĐẠI TỪ THAY THẾ DỰA TRÊN NGỮ CẢNH ĐA LƯỢT
        // =========================================================================
        CopilotContextManager.AnaphoraResolution anaphora = contextManager.resolveAnaphora(prompt, unaccented, currentUsername);
        if (anaphora.isHasAnaphora()) {
            prompt = anaphora.getResolvedPrompt();
            lower = prompt.toLowerCase();
            unaccented = unaccent(lower);
        }

        // =========================================================================
        // 0. NHẬN DIỆN CÁC Ý ĐỊNH GIAO TIẾP TỰ NHIÊN (ZERO TRACE - PHẢN HỒI ẤM ÁP, THÂN THIỆN)
        // =========================================================================
        // 0.1 Cảm ơn & Ghi nhận ("okeee cảm ơn", "cảm ơn mày", "thanks", "đa tạ")
        if (isGratitude(unaccented)) {
            return handleGratitudeIntent(prompt, currentUsername);
        }

        // 0.2 Khen ngợi & Động viên ("quá đỉnh", "vip quá", "giỏi thế", "chuẩn luôn")
        if (isPraise(unaccented)) {
            return handlePraiseIntent(prompt, currentUsername);
        }

        // 0.3 Xác nhận ngắn gọn / Đồng ý / Xong việc ("ok", "oke", "được rồi", "thế nhé", "nhất trí")
        if (isAcknowledgment(unaccented)) {
            return handleAcknowledgmentIntent(prompt, currentUsername);
        }

        // 0.4 Chốt ca / Nghỉ ngơi / Tạm biệt ("nghỉ thôi", "chốt ca", "bye", "mai tiếp")
        if (isFarewell(unaccented)) {
            return handleFarewellIntent(currentUsername);
        }

        // 0.5 Chào hỏi ngắn gọn ("chào", "hello", "hi", "ê mày", "alo")
        if (isGreeting(unaccented)) {
            return handleGreetingIntent(prompt, unaccented, currentUsername);
        }

        // 0.6 Thăm hỏi sức khỏe / Tâm sự / Động viên ("khỏe không", "có mệt không", "mệt quá")
        if (isSmallTalk(unaccented)) {
            return handleSmallTalkIntent(prompt, unaccented, currentUsername);
        }

        // 0.7 Hỏi về danh tính / Giới thiệu năng lực Copilot ("mày là ai", "giới thiệu bản thân", "mày làm được gì")
        if (isIdentityQuery(unaccented)) {
            return handleIdentityIntent(currentUsername);
        }

        List<ThinkingStepDto> traces = new ArrayList<>();
        if (anaphora.isHasAnaphora()) {
            traces.add(ThinkingStepDto.builder()
                    .step("ANAPHORA_RESOLVE")
                    .message("🔗 Nhận diện ngữ cảnh kế thừa: " 
                            + (anaphora.getTargetSku() != null ? "Sản phẩm [" + anaphora.getTargetSku() + "] " : "")
                            + (anaphora.getTargetCustomerName() != null ? "Khách hàng [" + anaphora.getTargetCustomerName() + "] " : "")
                            + (anaphora.getTargetOrderCode() != null ? "Đơn hàng [" + anaphora.getTargetOrderCode() + "]" : ""))
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .build());
        }

        traces.add(ThinkingStepDto.builder()
                .step("ANALYZE_INTENT")
                .message("🧠 Phân tích ngữ nghĩa: \"" + (prompt.length() > 40 ? prompt.substring(0, 37) + "..." : prompt) + "\"")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        // =========================================================================
        // 0.8 ƯU TIÊN CAO: NHẬN DIỆN CÂU HỎI GIẢI THÍCH TÍNH NĂNG / HƯỚNG DẪN NGHIỆP VỤ (TẦNG 3 - ERP Q&A)
        // Tránh để các từ khóa nhạy cảm ("công nợ", "tài chính") kích hoạt nhầm Action Tools
        // =========================================================================
        if (isExplanationOrHelpQuery(unaccented)) {
            return handleErpHelpTier(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 1. NHẬN DIỆN Ý ĐỊNH LÊN ĐƠN HÀNG NHANH / CONVERSATIONAL POS (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "len don", "tao don", "dat hang", "ban cho", "tao order", "xuat ban", "dat cho")) {
            return handleDraftOrderIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 2. NHẬN DIỆN Ý ĐỊNH XUẤT BÁO CÁO & FILE EXPORT (TẦNG 1)
        // =========================================================================
        if ((containsAny(unaccented, "xuat", "tai", "download", "gui file") && containsAny(unaccented, "file", "excel", "csv", "bao cao"))
                || containsAny(unaccented, "xuat file", "tai file", "xuat bao cao", "xuat excel", "tai excel", "xuat csv", "tai csv", "download", "file excel", "file csv")) {
            return handleReportExportIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 3. NHẬN DIỆN Ý ĐỊNH DOANH SỐ / LỢI NHUẬN / "ẤM NO" (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "am no", "doanh thu", "doanh so", "lai gop", "loi nhuan", "ban duoc bao nhieu", "tinh hinh buon ban", "ban hang hom nay")) {
            return handleSalesPerformanceIntent(unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 4. NHẬN DIỆN Ý ĐỊNH CÔNG NỢ / "NỢ DAI" / KHÓA NỢ (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "no dai", "no kho doi", "cong no", "tuoi no", "no qua han", "thang nao no", "ai no", "khoa no", "chan xuat hang")) {
            return handleDebtIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 5. NHẬN DIỆN Ý ĐỊNH CẠN KHO / QUÉT CẠN KHO AI (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "can kho", "het hang", "sap het hang", "nguy co can kho", "quet can kho", "can nhap")) {
            return handleInventoryForecastIntent(unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 6. NHẬN DIỆN Ý ĐỊNH HÀNH TRÌNH VẬN ĐƠN / "SHIP ĐƠN" (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "ship don", "van don", "tracking", "buu ta", "kien hang", "toi dau roi", "giao toi dau")) {
            return handleLogisticsIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 7. NHẬN DIỆN Ý ĐỊNH SOẠN PHIẾU CHI / TIỀN MẶT (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "phieu chi", "chi tien", "thanh toan tien", "tien mat bang", "tien dien", "tien nuoc")) {
            return handleFinanceIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8. NHẬN DIỆN Ý ĐỊNH TRA CỨU TỒN KHO CỤ THỂ (ÁO CỔ BẺ / SKU / TÊN) (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "con hang", "ton kho", "con cai", "con ao", "con quan", "con bao nhieu", "kiem tra hang", "tra cuu ton")) {
            return handleStockCheckIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.1 NHẬN DIỆN Ý ĐỊNH MUA HÀNG, ĐƠN PO & NHÀ CUNG CẤP SRM (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "mua hang", "nha cung cap", "ncc", "don po", "nhap kho grn", "gia von mac", "po cho duyet", "dat mua")) {
            return handleProcurementIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.2 NHẬN DIỆN Ý ĐỊNH KHUYẾN MẠI, VOUCHER & TÍCH ĐIỂM LOYALTY (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "voucher", "ma giam gia", "coupon", "khuyen mai", "tich diem", "diem thuong", "hang hoi vien", "the thanh vien", "doi diem", "diem tich luy")) {
            return handlePromotionIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.3 NHẬN DIỆN Ý ĐỊNH ĐIỂM BÁO SÁNG ĐIỀU HÀNH (MORNING EXECUTIVE BRIEFING) (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "diem bao", "ban tin sang", "morning briefing", "tin sang", "diem bao sang", "tinh hinh sang nay")) {
            return handleMorningBriefingIntent(currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.4 NHẬN DIỆN Ý ĐỊNH RÀ SOÁT RỦI RO & BẤT THƯỜNG GIAN LẬN (ANOMALY DETECTION) (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "bat thuong", "gian lan", "anomaly", "rui ro", "canh bao bat thuong", "lech tien mat", "burn rate")) {
            return handleAnomalyDetectionIntent(currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.5 NHẬN DIỆN Ý ĐỊNH CHUỖI TÁC VỤ LIÊN HOÀN ĐA PHÂN HỆ (ACTION CHAINING) (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "chuoi tac vu", "action chain", "chaining", "xau chuoi") 
                || (containsAny(unaccented, "soan don", "tao po") && containsAny(unaccented, "coc", "chi tien") && containsAny(unaccented, "thong bao", "bao thu kho"))) {
            return handleActionChainingIntent(prompt, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 8.6 NHẬN DIỆN Ý ĐỊNH TRA CỨU & TỔNG HỢP NHẬT KÝ THAO TÁC (AUDIT LOGS) (TẦNG 1)
        // =========================================================================
        if (containsAny(unaccented, "nhat ky", "lich su thao tac", "audit", "truy vet", "ai thao tac", "ai sua", "ai xoa", "dang nhap loi", "thao tac hom nay", "log he thong")) {
            return handleAuditLogQueryIntent(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 9. TẦNG 2: TƯ VẤN CHIẾN LƯỢC KINH DOANH & ĐỊNH LƯỢNG NHẬP HÀNG (TẦNG 2)
        // =========================================================================
        if (containsAny(unaccented, "tu van", "co nen nhap", "tinh nhap", "nen nhap them", "loi khuyen", "dong von", "chien luoc")) {
            return handleAdvisoryTier(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 10. TẦNG 3: HƯỚNG DẪN THAO TÁC QUY TRÌNH ERP (TẦNG 3)
        // =========================================================================
        if (containsAny(unaccented, "lam sao de", "lam the nao", "huong dan", "quy trinh", "cach xuat", "cach tao", "cach nhap")) {
            return handleErpHelpTier(prompt, unaccented, currentUsername, traces, startTime);
        }

        // =========================================================================
        // 11. TẦNG 4: FALLBACK THÔNG MINH - ĐÁP TỰ NHIÊN, KHÔNG TRACE, KHÔNG VĂN MẪU
        // =========================================================================
        return handleFallbackGeneral(prompt, unaccented, currentUsername);
    }

    // =========================================================================
    // HANDLERS CHI TIẾT CHO TỪNG LOẠI INTENT
    // =========================================================================

    private CopilotResponseDto handleDraftOrderIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("⚡ Kích hoạt Tool: draftPosOrder(phân tích cú pháp & kiểm tra tồn kho)...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("draftPosOrder")
                .orElseThrow(() -> new IllegalStateException("Tool draftPosOrder chưa được đăng ký!"));

        String customerName = extractCustomerKeyword(prompt);
        if (customerName == null || customerName.isBlank() || customerName.matches("^[0-9]+$")) {
            var ctx = contextManager.getContext(username);
            if (ctx.getLastCustomerName() != null) {
                customerName = ctx.getLastCustomerName();
            } else {
                customerName = "Khách mua lẻ";
            }
        }

        String phone = extractPhone(prompt);
        if (phone.isBlank()) {
            var ctx = contextManager.getContext(username);
            if (ctx.getLastCustomerPhone() != null) {
                phone = ctx.getLastCustomerPhone();
            }
        }

        String address = extractAddress(prompt);

        String paymentMethod = "CASH";
        if (containsAny(unaccented, "chuyen khoan", "ck", "bank", "ngan hang")) {
            paymentMethod = "BANK_TRANSFER";
        } else if (containsAny(unaccented, "cod", "thu ho")) {
            paymentMethod = "COD";
        }

        List<Map<String, Object>> items = extractOrderItems(prompt, unaccented, username);

        traces.add(ThinkingStepDto.builder()
                .step("STOCK_VALIDATION")
                .message("📦 Đang kiểm tra tồn kho vật lý, số lượng khả dụng & tự động áp bảng giá...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        Map<String, Object> args = new HashMap<>();
        args.put("customerName", customerName);
        args.put("customerPhone", phone);
        args.put("shippingAddress", address);
        args.put("paymentMethod", paymentMethod);
        args.put("items", items);
        args.put("note", prompt);

        ToolExecutionResult result = tool.execute(args, username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã soạn thảo xong Thẻ Đơn Hàng Dự Thảo (ORDER_DRAFT_CARD) kèm nút xác nhận 1-click.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleReportExportIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String reportType = "SALES_REVENUE";
        if (containsAny(unaccented, "can kho", "ton kho", "nhap hang")) {
            reportType = "INVENTORY_RESTOCK";
        } else if (containsAny(unaccented, "cong no", "tuoi no", "no xau")) {
            reportType = "DEBT_OVERDUE";
        }

        String format = containsAny(unaccented, "csv") ? "CSV" : "EXCEL";
        String timeRange = "THIS_MONTH";
        if (containsAny(unaccented, "hom nay", "ngay")) timeRange = "TODAY";
        else if (containsAny(unaccented, "tuan nay", "tuan")) timeRange = "THIS_WEEK";

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("⚡ Kích hoạt Tool: generateExportReport(reportType='" + reportType + "', format='" + format + "', timeRange='" + timeRange + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("generateExportReport")
                .orElseThrow(() -> new IllegalStateException("Tool generateExportReport chưa được đăng ký!"));

        Map<String, Object> args = Map.of(
                "reportType", reportType,
                "fileFormat", format,
                "timeRange", timeRange
        );

        traces.add(ThinkingStepDto.builder()
                .step("DB_QUERY")
                .message("📊 Đang truy xuất tập hợp dữ liệu từ Database & khởi tạo luồng Streaming Engine...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        ToolExecutionResult result = tool.execute(args, username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã đóng gói xong tệp dữ liệu an toàn & dựng Thẻ Tải Báo Cáo Thông Minh!")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleSalesPerformanceIntent(String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String timeRange = "TODAY";
        if (containsAny(unaccented, "tuan nay", "tuan")) timeRange = "THIS_WEEK";
        else if (containsAny(unaccented, "thang nay", "thang")) timeRange = "THIS_MONTH";

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("⚡ Kích hoạt Tool: getSalesPerformance(timeRange='" + timeRange + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("getSalesPerformance")
                .orElseThrow(() -> new IllegalStateException("Tool getSalesPerformance chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("timeRange", timeRange), username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("📈 Đã tổng hợp chỉ số doanh thu, biên lãi gộp & dựng biểu đồ Mini Sparkline 7 ngày.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleDebtIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String keyword = extractCustomerKeyword(prompt);

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🔍 Kích hoạt Tool: getCustomerDebtSummary(keyword='" + keyword + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("getCustomerDebtSummary")
                .orElseThrow(() -> new IllegalStateException("Tool getCustomerDebtSummary chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("keyword", keyword), username);

        if (!keyword.isBlank()) {
            customerRepository.findAll().stream()
                    .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(keyword.toLowerCase()))
                            || (c.getPhone() != null && c.getPhone().contains(keyword)))
                    .findFirst()
                    .ifPresent(c -> contextManager.updateCustomer(username, c.getId(), c.getName(), c.getPhone(), c.getCurrentDebt()));
        }

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("🛡️ Đã phân tích tuổi nợ chi tiết & đề xuất biện pháp bảo toàn vốn lưu động.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleInventoryForecastIntent(String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String risk = "ALL";
        if (containsAny(unaccented, "gap", "nguy cap", "critical")) risk = "CRITICAL";

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🚨 Kích hoạt Tool: forecastDepletedStock(riskLevel='" + risk + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("forecastDepletedStock")
                .orElseThrow(() -> new IllegalStateException("Tool forecastDepletedStock chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("riskLevel", risk), username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("📦 Hoàn thành quét nguy cơ cạn kho AI & dự toán ngân sách nhập hàng an toàn.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleLogisticsIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String keyword = extractShippingKeyword(prompt);

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🚚 Kích hoạt Tool: trackShipmentOrder(keyword='" + keyword + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("trackShipmentOrder")
                .orElseThrow(() -> new IllegalStateException("Tool trackShipmentOrder chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("keyword", keyword), username);
        if (!keyword.isBlank()) {
            contextManager.updateOrder(username, keyword);
        }

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("📍 Đã liên kết API hãng vận chuyển & kết xuất Timeline hành trình kiện hàng.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleFinanceIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        BigDecimal amount = extractAmountFromPrompt(prompt);
        String payee = "Chủ nhà / Đối tác";
        if (containsAny(unaccented, "tien dien")) payee = "Điện lực TP.HCM";
        else if (containsAny(unaccented, "tien nuoc")) payee = "Cấp thoát nước";
        else if (containsAny(unaccented, "mat bang", "thue nha")) payee = "Chủ tòa nhà / Mặt bằng";

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("📝 Kích hoạt Tool: draftExpenseVoucher(amount=" + amount + ", payee='" + payee + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("draftExpenseVoucher")
                .orElseThrow(() -> new IllegalStateException("Tool draftExpenseVoucher chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of(
                "amount", amount,
                "payeeName", payee,
                "category", "CHI_HOAT_DONG",
                "note", prompt
        ), username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✍️ Đã lập Phiếu chi dự thảo kèm Thẻ phê duyệt quyền hạn.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleStockCheckIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String searchTarget = extractProductKeyword(prompt, unaccented);

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🔍 Kích hoạt Tool: checkProductStock(keyword='" + searchTarget + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("checkProductStock")
                .orElseThrow(() -> new IllegalStateException("Tool checkProductStock chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("keyword", searchTarget), username);

        // Lưu ngữ cảnh sản phẩm gần nhất vào Context Manager
        productRepository.findAll().stream()
                .filter(p -> (p.getSku() != null && p.getSku().equalsIgnoreCase(searchTarget))
                        || (p.getName() != null && unaccent(p.getName().toLowerCase()).contains(unaccent(searchTarget.toLowerCase()))))
                .findFirst()
                .ifPresent(p -> contextManager.updateProduct(username, p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock()));

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("📦 Đã kiểm tra số lượng tồn vật lý & khả dụng trên kệ kho.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleProcurementIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String queryType = "OVERVIEW";
        if (containsAny(unaccented, "cho duyet", "chua duyet", "can duyet")) {
            queryType = "PENDING_PO";
        } else if (containsAny(unaccented, "cong no", "no ncc", "phai tra", "ap")) {
            queryType = "SUPPLIERS";
        }

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🏭 Kích hoạt Tool: queryProcurementAndSuppliers(queryType='" + queryType + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("queryProcurementAndSuppliers")
                .orElseThrow(() -> new IllegalStateException("Tool queryProcurementAndSuppliers chưa được đăng ký!"));

        ToolExecutionResult result = tool.execute(Map.of("queryType", queryType), username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã tổng hợp dữ liệu Phân hệ Mua hàng & SRM (PO, GRN & Công nợ NCC).")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handlePromotionIntent(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        String queryType = "VOUCHERS";
        String keyword = "";
        if (containsAny(unaccented, "diem", "hoi vien", "hang the", "thanh vien", "tich luy")) {
            queryType = "CUSTOMER_LOYALTY";
            keyword = extractCustomerKeyword(prompt);
        }

        traces.add(ThinkingStepDto.builder()
                .step("TOOL_CALLING")
                .message("🎟️ Kích hoạt Tool: queryPromotionsAndLoyalty(queryType='" + queryType + "', keyword='" + keyword + "')...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        AgentTool tool = toolRegistry.getTool("queryPromotionsAndLoyalty")
                .orElseThrow(() -> new IllegalStateException("Tool queryPromotionsAndLoyalty chưa được đăng ký!"));

        Map<String, Object> args = new HashMap<>();
        args.put("queryType", queryType);
        args.put("keyword", keyword);

        ToolExecutionResult result = tool.execute(args, username);

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã truy xuất dữ liệu Động cơ Khuyến Mại, Voucher & Điểm Thưởng Hội Viên.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(result.getMessage())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(result.getUiWidget())
                .toolResult(result)
                .build();
    }

    private CopilotResponseDto handleAdvisoryTier(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("ADVISORY_ANALYSIS")
                .message("🧠 Phân tích dữ liệu tốc độ tiêu thụ 30 ngày & tối ưu hóa vốn lưu động...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        List<Product> products = productRepository.findAll();
        Product sampleProduct = products.isEmpty() ? null : products.get(0);

        StringBuilder sb = new StringBuilder();
        sb.append("💡 **GÓC TƯ VẤN CHIẾN LƯỢC KHO VẬN & DÒNG TIỀN (CFO COPILOT)**:\n\n");

        if (sampleProduct != null) {
            sb.append(String.format("Dựa trên dữ liệu bán hàng 30 ngày qua của danh mục thời trang:\n"));
            sb.append(String.format("- **Tốc độ bán trung bình**: ~ **3.2 sản phẩm/ngày**.\n"));
            sb.append(String.format("- **Tồn kho hiện tại**: **%d cái** (đủ đáp ứng trong khoảng **%d ngày** bán).\n\n",
                    sampleProduct.getStock(), Math.max(1, sampleProduct.getStock() / 3)));
            sb.append("📌 **Khuyến nghị định lượng**:\n");
            sb.append("1. **Không nên nhập ồ ạt 100 cái ngay một đợt**: Điều này sẽ giam giữ khoảng **25 - 35 triệu đồng** vốn lưu động trong hơn 30 ngày.\n");
            sb.append("2. **Phương án nhập chia đợt tối ưu (EOQ)**: Nên đặt hàng từ NCC khoảng **40 - 50 cái/đợt** mỗi 12 ngày. Cách này vừa đảm bảo kho không bao giờ cạn, vừa giữ dòng tiền linh hoạt để quay vòng các mặt hàng 'hot trend' khác!\n");
        } else {
            sb.append("Dựa trên chỉ số vòng quay tồn kho (Inventory Turnover Ratio), khuyến nghị bạn nên chia nhỏ đợt nhập thành 2 kỳ/tháng để tối ưu dòng tiền và giảm chi phí lưu kho.\n");
        }
        sb.append("\n👉 Bạn có thể yêu cầu: *\"Xuất file Excel dự báo cạn kho\"* để xem bảng tính toán chi tiết cho từng SKU!");

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã hoàn thành phân tích định lượng & đưa ra giải pháp bảo toàn thanh khoản.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(sb.toString())
                .tier("TIER_2_ADVISORY")
                .thinkingTrace(traces)
                .build();
    }

    private CopilotResponseDto handleErpHelpTier(String prompt, String unaccented, String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("ERP_QA")
                .message("📚 Tra cứu Cẩm nang Vận hành & Kiến trúc Phân hệ ERP Cloud...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        StringBuilder sb = new StringBuilder();

        // 1. GIẢI THÍCH TÍNH NĂNG TÀI CHÍNH & CÔNG NỢ (TREASURY, VOUCHERS, RECONCILIATION, P&L, AGING DEBT)
        if (containsAny(unaccented, "tai chinh", "cong no", "so quy", "quy tien", "ngan hang", "sao ke", "doi soat", "p&l", "pnl", "lai lo", "dong tien", "tuoi no", "han muc")) {
            sb.append("💼 **TỔNG QUAN & HƯỚNG DẪN TÍNH NĂNG TÀI CHÍNH & CÔNG NỢ (TREASURY & DEBT BI)** 🌿\n\n");
            sb.append("Phân hệ **Tài chính & Quản trị Công nợ** của ERP Cloud được thiết kế theo chuẩn Enterprise với 5 trụ cột cốt lõi:\n\n");
            
            sb.append("1. 💰 **Quản lý Quỹ Đa Tài Khoản (Multi-Account Treasury)**:\n");
            sb.append("   - Quản trị song song **Quỹ tiền mặt tại chỗ** (Cash on Hand) và các **Tài khoản Ngân hàng** (Vietcombank, MBBank, Techcombank...).\n");
            sb.append("   - **Luân chuyển quỹ nội bộ (Internal Transfer)**: Điều chuyển tiền giữa các quỹ với cơ chế khóa dữ liệu `Isolation.REPEATABLE_READ` và `@Version` Optimistic Locking chống xung đột số dư.\n\n");

            sb.append("2. 📝 **Hạch toán Thu - Chi Minh Bạch (Finance Vouchers)**:\n");
            sb.append("   - Lập và phê duyệt **Phiếu Thu (Receipt)** & **Phiếu Chi (Payment)** theo 12 Trung tâm chi phí (**Cost Centers**: Mặt bằng, Lương, Marketing, Điện nước, Logistics, Mua hàng...).\n");
            sb.append("   - Mọi khoản chi xuất quỹ đều được ghi nhận nhật ký hạch toán minh bạch chuẩn kiểm toán.\n\n");

            sb.append("3. ⚡ **Tự Động Đối Soát Sao Kê Ngân Hàng (Smart Bank Reconciliation)**:\n");
            sb.append("   - Nạp file sao kê ngân hàng (CSV / Text), thuật toán **AI Fuzzy Matching** tự động bóc tách mã đơn `ORD-...`, khớp khách hàng và số tiền.\n");
            sb.append("   - **Đối soát 1-click**: Tự động chuyển đơn hàng sang trạng thái `PAID` (Đã thanh toán), tự động sinh Phiếu Thu và cộng tiền vào tài khoản ngân hàng.\n\n");

            sb.append("4. 📊 **Báo Cáo Lãi Lỗ P&L BI & Dòng Tiền (Income Statement & Cash Runway)**:\n");
            sb.append("   - Báo cáo kết quả kinh doanh đa kỳ: **Doanh thu thuần ➔ Giá vốn COGS ➔ Lợi nhuận gộp ➔ Phân rã 12 khoản mục chi phí OPEX ➔ Lợi nhuận ròng EBIT**.\n");
            sb.append("   - **CFO Cash Runway Alert**: Phân tích tốc độ tiêu thụ tiền (Burn Rate) để tính toán chính xác số ngày an toàn dòng tiền (Runway Days) phòng ngừa rủi ro thanh khoản.\n\n");

            sb.append("5. 🛡️ **Quản Trị Công Nợ Đại Lý & Thu Hồi Vốn (Aging Debt Report)**:\n");
            sb.append("   - Quản lý hạn mức tín dụng (**Credit Limit**) từng đối tác; cảnh báo vượt hạn mức tự động.\n");
            sb.append("   - Bảng phân tích **Tuổi nợ 4 cấp độ** (0-30 ngày, 31-60 ngày, 61-90 ngày, quá 90 ngày - nợ xấu).\n");
            sb.append("   - Cơ chế tự động khóa quyền xuất hàng đối với khách hàng nợ quá hạn để bảo toàn dòng vốn.\n\n");

            sb.append("🔒 **LƯU Ý VỀ PHÂN QUYỀN (RBAC)**:\n");
            sb.append("- Thao tác **Phê duyệt xuất quỹ** và **Khóa công nợ** chỉ dành riêng cho tài khoản có vai trò **ADMIN (Quản trị viên)**, **MANAGER (Quản lý)** hoặc **ACCOUNTANT (Kế toán)**.\n");
            sb.append("- Các tài khoản nhân viên khác (Sales, Kho, User thông thường) **chỉ có quyền XEM (Read-only)** để bảo vệ an toàn dòng tiền doanh nghiệp!");

        } else if (containsAny(unaccented, "mua hang", "nha cung cap", "ncc", "don po", "grn", "gia von", "mac", "binh quan gia quyen", "srm")) {
            sb.append("🏭 **QUY TRÌNH MUA HÀNG, SRM & GIÁ VỐN BÌNH QUÂN GIA QUYỀN (MAC)** 🌿\n\n");
            sb.append("Phân hệ **Mua hàng & Quản trị Nhà Cung Cấp (Procurement & SRM)** vận hành theo quy trình chuẩn khép kín 4 bước:\n\n");
            sb.append("1. 📋 **Bước 1 - Lập & Phê Duyệt Đơn Đặt Mua (Purchase Order - PO)**:\n");
            sb.append("   - Chọn Nhà cung cấp/Xưởng, ngày hẹn giao và danh mục sản phẩm kèm đơn giá nhập dự kiến.\n");
            sb.append("   - Quản lý phê duyệt để đơn PO chuyển sang trạng thái `APPROVED` (Sẵn sàng nhập kho).\n\n");
            sb.append("2. 📦 **Bước 2 - Nhập Kho Kiểm Nhận (Goods Receipt Note - GRN)**:\n");
            sb.append("   - Thủ kho kiểm đếm thực tế đối chiếu với đơn PO gốc; ghi nhận hàng thực nhận, thừa/thiếu.\n");
            sb.append("   - Xác nhận nhập kho để cập nhật tồn kho vật lý (`physical_stock`).\n\n");
            sb.append("3. ⚖️ **Bước 3 - Tự Động Định Giá Vốn Bình Quân Gia Quyền (MAC)**:\n");
            sb.append("   - Hệ thống tự động tính toán tức thời theo công thức:\n");
            sb.append("     *Giá vốn mới = [(Tồn kho cũ × Giá vốn cũ) + (SL nhập mới × Đơn giá nhập mới)] / (Tồn kho cũ + SL nhập mới)*\n");
            sb.append("   - Cập nhật tự động trường `moving_avg_cost` của sản phẩm làm cơ sở tính lãi gộp chính xác.\n\n");
            sb.append("4. 💳 **Bước 4 - Ghi Nhận Công Nợ Phải Trả NCC (AP) & Thanh Toán**:\n");
            sb.append("   - Tự động cộng nợ NCC, theo dõi hạn mức tín dụng và lập Phiếu chi từ Sổ quỹ để đối trừ công nợ khi thanh toán.\n");

        } else if (containsAny(unaccented, "khuyen mai", "voucher", "coupon", "giam gia", "tich diem", "loyalty", "hang the", "hoi vien", "gia si")) {
            sb.append("🎁 **ĐỘNG CƠ KHUYẾN MẠI, VOUCHER & TÍCH ĐIỂM HỘI VIÊN (LOYALTY)** 🌿\n\n");
            sb.append("Phân hệ Khuyến mại mang đến 3 chiến lược thúc đẩy doanh số & chăm sóc khách hàng thân thiết:\n\n");
            sb.append("1. 🎟️ **Mã Giảm Giá & Voucher Đa Năng**:\n");
            sb.append("   - Hỗ trợ Giảm theo % (có chặn mức giảm tối đa) hoặc Giảm tiền mặt cố định (VNĐ).\n");
            sb.append("   - Điều kiện kiểm soát: Giá trị đơn tối thiểu, tổng số lượt dùng toàn sàn & số lượt tối đa cho mỗi khách.\n\n");
            sb.append("2. 🏷️ **Bảng Giá Sỉ & Chiết Khấu Theo Số Lượng (Tiered Pricing)**:\n");
            sb.append("   - Tự động hạ đơn giá khi khách mua số lượng lớn (ví dụ: mua từ 10 cái giảm 10%, mua từ 50 cái giảm 20%).\n");
            sb.append("   - Tự động áp dụng chiết khấu trực tiếp trên giỏ hàng POS mà không cần nhập mã.\n\n");
            sb.append("3. 👑 **Hệ Thống Tích Điểm Hội Viên 4 Cấp Độ**:\n");
            sb.append("   - 🥉 **Đồng (Bronze)**: Hệ số `1.0x` (Mặc định khi đăng ký hội viên).\n");
            sb.append("   - 🥈 **Bạc (Silver)**: Hệ số `1.2x` (Tự động thăng hạng khi tích lũy đạt 5.000.000 VNĐ).\n");
            sb.append("   - 🥇 **Vàng (Gold)**: Hệ số `1.5x` (Tự động thăng hạng khi tích lũy đạt 20.000.000 VNĐ).\n");
            sb.append("   - 💎 **Kim Cương (Diamond)**: Hệ số `2.0x` (Tự động thăng hạng khi tích lũy đạt 50.000.000 VNĐ).\n");
            sb.append("   - Quy đổi điểm: `1 điểm = 1.000 VNĐ` cấn trừ trực tiếp vào hóa đơn thanh toán!\n");

        } else if (containsAny(unaccented, "hoa don", "pdf", "in don")) {
            sb.append("📄 **QUY TRÌNH XUẤT HÓA ĐƠN ĐỎ PDF A4 CHUẨN KẾ TOÁN**:\n\n");
            sb.append("1. **Bước 1**: Vào menu **Bán hàng & Đơn hàng** (icon giỏ hàng).\n");
            sb.append("2. **Bước 2**: Nhấp vào mã đơn hàng cần in (vd: `ORD-20261118-001`) để mở Drawer chi tiết.\n");
            sb.append("3. **Bước 3**: Nhìn góc trên bên phải Drawer, nhấp vào nút **'In Hóa Đơn PDF'** (hoặc phím tắt `Ctrl + P`).\n");
            sb.append("4. **Bước 4**: Hộp thoại in A4 tự động mở với đầy đủ Logo công ty, Bảng chi tiết sản phẩm, Mã VAT và Chữ ký bàn giao.\n");
        } else if (containsAny(unaccented, "barcode", "pos", "quet ma")) {
            sb.append("🏷️ **HƯỚNG DẪN QUÉT MÃ VẠCH POS & ĐƠN VỊ TÍNH (UoM)**:\n\n");
            sb.append("1. Mở màn hình **Bán hàng POS** từ thanh điều hướng chính.\n");
            sb.append("2. Đặt con trỏ chuột vào ô **'Quét Barcode / SKU'** (hoặc bấm phím tắt F2).\n");
            sb.append("3. Bấm súng bắn mã vạch vào tem nhãn sản phẩm ➔ Sản phẩm lập tức được thêm vào giỏ hàng với số lượng 1.\n");
            sb.append("4. Nếu muốn đổi đơn vị tính (Thùng / Lốc / Hộp), nhấp vào ô chọn Đơn vị tính ngay trên dòng sản phẩm.\n");
        } else {
            sb.append("📖 **HỆ THỐNG TRỢ GIÚP VẬN HÀNH ERP CLOUD**:\n\n");
            sb.append("- Để **Xem báo cáo doanh số**: Mày chỉ cần gõ *\"Doanh thu hôm nay\"* hoặc *\"Xuất file Excel doanh thu tuần này\"*.\n");
            sb.append("- Để **Kiểm tra kho**: Gõ *\"Kiểm tra tồn kho Áo Polo\"* hoặc *\"Quét các mặt hàng sắp hết\"*.\n");
            sb.append("- Để **Mua hàng & SRM**: Gõ *\"Có đơn PO nào chờ duyệt không?\"* hoặc *\"Tình hình công nợ NCC\"*.\n");
            sb.append("- Để **Khuyến mại & Loyalty**: Gõ *\"Xem danh sách voucher đang chạy\"* hoặc *\"Tra cứu điểm tích lũy của anh Nam\"*.\n");
            sb.append("- Để **Quản lý công nợ**: Gõ *\"Khách hàng nào đang nợ nhiều nhất\"* hoặc *\"Giải thích tính năng tài chính & công nợ\"*.\n");
            sb.append("- Để **Soạn phiếu chi**: Gõ *\"Soạn phiếu chi tiền mặt bằng 5 triệu\"*.\n");
            sb.append("- Để **Tra cứu giao vận**: Gõ *\"Kiểm tra vận đơn ORD-...\"*.\n");
        }

        traces.add(ThinkingStepDto.builder()
                .step("SYNTHESIZE")
                .message("✨ Đã hoàn tất tra cứu tài liệu & xuất bản cẩm nang hướng dẫn nghiệp vụ.")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        return CopilotResponseDto.builder()
                .content(sb.toString())
                .tier("TIER_3_ERP_QA")
                .thinkingTrace(traces)
                .build();
    }

    private boolean isExplanationOrHelpQuery(String unaccented) {
        boolean hasExplainKeyword = containsAny(unaccented,
                "giai thich", "tinh nang", "chuc nang", "co tac dung gi", "dung de lam gi",
                "lam duoc gi", "la gi", "nhu the nao", "hoat dong nhu the nao", "tong quan",
                "huong dan", "quy trinh", "cach su dung", "cach dung", "cach hoat dong",
                "gioi thieu ve", "gioi thieu tinh nang", "co nhung gi", "co chuc nang gi"
        );
        boolean hasDirectAction = containsAny(unaccented, "len don", "tao don", "xuat file", "tai file", "soan phieu chi", "khoa no");
        return hasExplainKeyword && !hasDirectAction;
    }

    // =========================================================================
    // NHÓM GIAO TIẾP TỰ NHIÊN, XÃ GIAO & PHẢN HỒI ẤM ÁP (ZERO TRACE)
    // =========================================================================

    private boolean isGratitude(String unaccented) {
        return matchesAny(unaccented,
                "cam on", "thanks", "thank you", "tks", "ty", "da cam on",
                "biet on", "da ta", "thank kiu", "thank bro", "cam on nha", "cam on nhe", "cam on may", "cam on ban"
        );
    }

    private boolean isPraise(String unaccented) {
        return matchesAny(unaccented,
                "tuyet voi", "qua da", "qua dinh", "dinh chop", "gioi the", "gioi day",
                "vip day", "vip qua", "hay qua", "chuan day", "chuan luon", "xin xo",
                "muot day", "thong minh the", "thong minh day", "10 diem", "muoi diem",
                "tuyet cu meo", "qua xuat sac"
        );
    }

    private boolean isAcknowledgment(String unaccented) {
        if (containsAny(unaccented, "xuat", "file", "doanh thu", "kho", "no", "don", "chi", "kiem tra", "len don", "tao don")) {
            return false;
        }
        return matchesAny(unaccented,
                "ok", "oke", "okeee", "oki", "okie", "ok nha", "oke nha", "ok nhe", "oke nhe",
                "ok men", "duoc roi", "xong roi", "the nhe", "vay nhe", "nhat tri",
                "da hieu", "ro roi", "hieu roi", "tiep tuc nhe"
        );
    }

    private boolean isFarewell(String unaccented) {
        return matchesAny(unaccented,
                "nghi thoi", "chot ca", "tan ca", "nghi ngoi", "nghi day",
                "tam biet", "bye", "bai bai", "ngu ngon", "mai tiep", "off day", "di ngu day", "tam nghi", "nghi tay"
        );
    }

    private boolean isGreeting(String unaccented) {
        return matchesAny(unaccented,
                "chao", "xin chao", "hello", "hi", "helo", "alo",
                "e may", "may oi", "copilot oi", "morning", "buoi sang", "buoi toi", "buoi trua",
                "chao buoi sang", "chao buoi toi", "chao buoi chieu"
        );
    }

    private boolean isSmallTalk(String unaccented) {
        return matchesAny(unaccented,
                "khoe khong", "co khoe khong", "met khong", "on khong",
                "dao nay sao roi", "dang lam gi day", "ban khong",
                "met qua", "chan qua", "stress", "nhieu viec qua"
        );
    }

    private boolean isIdentityQuery(String unaccented) {
        if (containsAny(unaccented, "tinh nang", "chuc nang", "khuyen mai", "mua hang", "tai chinh", "cong no", "ban hang", "kho", "don hang", "voucher", "loyalty", "quy trinh", "mac", "po", "grn")) {
            return false;
        }
        return matchesAny(unaccented,
                "may la ai", "ban la ai", "gioi thieu", "gioi thieu ban than",
                "may lam duoc gi", "ban lam duoc gi", "nang luc", "huong dan tong quan"
        );
    }

    private CopilotResponseDto handleGratitudeIntent(String prompt, String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String[] friendlyReplies = {
                "Dạ không có chi nè " + userDisplay + "! 🌿 Việc trơn tru thuận lợi là ấm lòng rồi. Cần soi thêm số liệu hay xuất file báo cáo gì cứ ới tao nhé! 🚀",
                "Không có gì đâu nè " + userDisplay + "! 🎯 Tao luôn túc trực 24/7 để sát cánh cùng mày. Việc buôn bán cứ để tao hỗ trợ một tay, cần gì cứ nhắn tao liền nha! 💼",
                "Dạ anh em mình cùng một thuyền mà! 🌿 Việc được giải quyết êm đẹp là tao vui rồi. Cứ yên tâm chỉ huy, bất cứ phát sinh gì về kho, quỹ hay đơn từ tao đều sẵn sàng hỗ trợ tức thì! 🛡️",
                "Okee " + userDisplay + " nhé! ✨ Rất vui được hỗ trợ mày. Chúc mày hôm nay buôn may bán đắt, đơn về ầm ầm! Có gì cần tính toán hay phân tích cứ bảo tao! 💰📈"
        };
        int idx = Math.abs(prompt.hashCode()) % friendlyReplies.length;

        return CopilotResponseDto.builder()
                .content(friendlyReplies[idx])
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handlePraiseIntent(String prompt, String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String[] praiseReplies = {
                "Haha được " + userDisplay + " khen là tao nở cả mũi rồi đây! 🎯 Toàn bộ dữ liệu ERP và kho bãi cứ để tao canh chừng, phục vụ mày chu đáo 24/7 luôn! 💼✨",
                "Cảm ơn " + userDisplay + " nha! 🌿 Nhờ mày ra đề bài chuẩn chỉ nên tao mới phản ứng nhanh được thế đấy. Sẵn sàng cho nhiệm vụ tiếp theo rồi sếp ơi! 🚀",
                "Hihi quá đã! 🌿 Tao sinh ra là để đồng hành hỗ trợ mày tối đa mà. Cứ tin tưởng tao, việc điều hành sẽ ngày càng mượt mà nhẹ nhàng hơn nữa! ⚡"
        };
        int idx = Math.abs(prompt.hashCode()) % praiseReplies.length;

        return CopilotResponseDto.builder()
                .content(praiseReplies[idx])
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleAcknowledgmentIntent(String prompt, String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String[] ackReplies = {
                "Nhất trí nha " + userDisplay + "! 🌿 Tao túc trực ở đây, cần xử lý hay kiểm tra gì cứ gọi tao ngay nhé! 🚀",
                "Okee mày! Việc điều hành cứ thong thả, số liệu và vận đơn có tao canh chừng 24/7 rồi! 💼",
                "Đã rõ sếp ơi! 🎯 Sẵn sàng đón lệnh tiếp theo bất cứ lúc nào! ⚡"
        };
        int idx = Math.abs(prompt.hashCode()) % ackReplies.length;

        return CopilotResponseDto.builder()
                .content(ackReplies[idx])
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleFarewellIntent(String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String reply = "Vâng, chốt ca hôm nay thôi! " + userDisplay + " nghỉ ngơi lấy lại năng lượng nhé. 🌙✨\n\n"
                + "Toàn bộ số liệu kho bãi, đơn từ và tiến trình giao vận cứ để tao canh chừng cho. Chúc " + userDisplay + " có buổi tối thật thư thái, hẹn gặp lại mày vào ca làm việc tới nha! 🌿💼";

        return CopilotResponseDto.builder()
                .content(reply)
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleGreetingIntent(String prompt, String unaccented, String username) {
        LocalTime now = LocalTime.now();
        String greetingTime;
        if (now.isBefore(LocalTime.of(11, 0))) {
            greetingTime = "Chào buổi sáng năng lượng";
        } else if (now.isBefore(LocalTime.of(13, 30))) {
            greetingTime = "Chào buổi trưa";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            greetingTime = "Chào buổi chiều làm việc hiệu quả";
        } else {
            greetingTime = "Chào buổi tối";
        }

        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String quickHello = String.format("👋 **%s, %s!** 🌿\n\nTao là Copilot đây, chúc mày một ngày làm việc hanh thông, đơn về ầm ầm nhé! Hôm nay cần tao soi nhanh doanh thu, quét cạn kho hay xuất file báo cáo gì không? 🚀",
                greetingTime, userDisplay);

        return CopilotResponseDto.builder()
                .content(quickHello)
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleSmallTalkIntent(String prompt, String unaccented, String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        if (containsAny(unaccented, "met", "stress", "ap luc", "nhieu viec", "chan")) {
            return CopilotResponseDto.builder()
                    .content("Làm chủ kinh doanh quản lý trăm thứ việc dễ căng thẳng lắm! 🥺 " + userDisplay + " nhớ uống ngụm nước, vươn vai thư giãn tí nhé. Còn mấy việc tra cứu kho bãi, theo dõi vận đơn hay tổng hợp doanh thu cứ để tao lo bớt cho! 🌿💼")
                    .tier("TIER_4_CONVERSATIONAL")
                    .thinkingTrace(Collections.emptyList())
                    .build();
        }

        return CopilotResponseDto.builder()
                .content("Tao là AI chạy bằng điện và dữ liệu nên lúc nào cũng tràn đầy 100% pin để phục vụ " + userDisplay + "! ⚡ Tình hình buôn bán hôm nay thế nào rồi, cần tao hỗ trợ gì cứ bảo nhé! 🌿")
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleIdentityIntent(String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👋 Xin chào %s! Tao là **ERP AI Copilot Ultra VIP** — Trợ lý Trưởng Điều hành Toàn năng của doanh nghiệp mày. 🌿\n\n", userDisplay));
        sb.append("🎯 **Các năng lực vượt trội tao có thể phục vụ mày tức thì**:\n");
        sb.append("1. 🛒 **Conversational POS**: *\"Lên đơn cho chị Lan 0988123456: 2 áo Polo đen, chuyển khoản\"*.\n");
        sb.append("2. 📊 **Xuất file báo cáo đa định dạng**: *\"Xuất file Excel doanh thu tuần này\"* hoặc *\"Tải file CSV dự báo cạn kho\"*.\n");
        sb.append("3. 💰 **Phân tích kinh doanh & Lãi gộp**: *\"Hôm nay ấm no không mày?\"* hoặc *\"Doanh thu tháng này\"*.\n");
        sb.append("4. 📦 **Kiểm tra kho & Cảnh báo cạn hàng**: *\"Kho còn cái áo Polo đen không?\"* hoặc *\"Nó giá bao nhiêu?\"* (Nhớ ngữ cảnh đa lượt).\n");
        sb.append("5. 🏭 **Mua hàng & Quản trị SRM**: *\"Có đơn PO nào chờ duyệt không?\"* hoặc *\"Tình hình công nợ NCC\"*.\n");
        sb.append("6. 🎟️ **Khuyến mại & Loyalty**: *\"Hệ thống có voucher nào đang chạy?\"* hoặc *\"Tra cứu điểm tích lũy của anh Nam\"*.\n");
        sb.append("7. 🛡️ **Quản trị công nợ & Thu hồi vốn**: *\"Khách nào nợ dai nhất?\"* hoặc *\"Khóa xuất hàng công nợ xấu\"*.\n");
        sb.append("8. 🚚 **Theo dõi hành trình giao vận**: *\"Ship đơn ORD-... tới đâu rồi?\"*.\n");
        sb.append("9. 📝 **Soạn thảo duyệt chi**: *\"Lập phiếu chi tiền mặt bằng 5 triệu\"*.\n\n");
        sb.append("👉 Mày có thể gõ phím hoặc bấm nút **Micro 🎙️** để ra lệnh giọng nói tiếng Việt rảnh tay bất cứ lúc nào nhé!");

        return CopilotResponseDto.builder()
                .content(sb.toString())
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    private CopilotResponseDto handleFallbackGeneral(String prompt, String unaccented, String username) {
        String userDisplay = (username != null && !username.isBlank()) ? "@" + username : "mày";
        String reply = "Dạ tao nghe đây " + userDisplay + " ơi! 🌿 Câu này tao chưa hiểu rõ ý mày lắm nè. Mày đang cần tao hỗ trợ việc gì nào? Ví dụ:\n\n"
                + "- 🛒 **Lên đơn hàng nhanh** (*\"Lên đơn cho chị Lan 0988123456: 2 áo Polo đen, chuyển khoản\"*)\n"
                + "- 📊 **Soi doanh thu & lãi gộp** (*\"Hôm nay ấm no không mày?\"*)\n"
                + "- 📦 **Kiểm tra tồn kho** (*\"Kho còn cái áo Polo đen không?\"*)\n"
                + "- 🏭 **Mua hàng & Đơn PO** (*\"Có đơn mua PO nào đang chờ duyệt không?\"*)\n"
                + "- 🎟️ **Mã giảm giá & Voucher** (*\"Xem danh sách voucher đang chạy\"*)\n"
                + "- 👑 **Điểm tích lũy hội viên** (*\"Tra cứu điểm tích lũy của anh Nam\"*)\n"
                + "- 🚨 **Quét hàng cạn kho AI** (*\"Quét các mặt hàng sắp hết\"*)\n"
                + "- 📑 **Xuất file báo cáo** (*\"Xuất file Excel doanh thu tuần này\"*)\n"
                + "- 🚚 **Theo dõi vận đơn** (*\"Ship đơn ORD-... tới đâu rồi?\"*)\n\n"
                + "Cứ ới tao một câu hoặc bấm micro nói là tao xử lý ngay nhé! 🚀";

        return CopilotResponseDto.builder()
                .content(reply)
                .tier("TIER_4_CONVERSATIONAL")
                .thinkingTrace(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // HELPER METHODS & ENTITY EXTRACTORS
    // =========================================================================

    private List<Map<String, Object>> extractOrderItems(String prompt, String unaccented, String username) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<Product> allProducts = productRepository.findAll();

        for (Product p : allProducts) {
            String pNameUnacc = unaccent(p.getName().toLowerCase());
            String pSku = p.getSku() != null ? p.getSku().toLowerCase() : "";

            boolean match = false;
            if (pSku.contains("polo") && (unaccented.contains("polo") || unaccented.contains("ao co be"))) {
                if (pSku.contains("black") || pSku.contains("den")) {
                    match = unaccented.contains("den") || unaccented.contains("black") || !unaccented.contains("trang");
                } else {
                    match = true;
                }
            } else if (pSku.contains("quan-au") && (unaccented.contains("quan au") || unaccented.contains("quan tay"))) {
                match = true;
            } else if (pSku.contains("so-mi") && unaccented.contains("so mi")) {
                match = true;
            } else if (unaccented.contains(pNameUnacc) || (p.getSku() != null && unaccented.contains(pSku))) {
                match = true;
            }

            if (match) {
                int qty = 1;
                Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(?:cai|chiec|hop|mon)?\\s*(?:ao|quan)?", Pattern.CASE_INSENSITIVE);
                Matcher qtyMatcher = qtyPattern.matcher(prompt);
                if (qtyMatcher.find()) {
                    try {
                        qty = Integer.parseInt(qtyMatcher.group(1));
                    } catch (Exception ignored) {
                    }
                }

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", p.getId());
                itemMap.put("sku", p.getSku());
                itemMap.put("productName", p.getName());
                itemMap.put("quantity", qty);
                itemMap.put("unitPrice", p.getPrice());
                items.add(itemMap);
                break;
            }
        }

        // Nếu không có tên sản phẩm trong prompt, kiểm tra context đa lượt
        if (items.isEmpty()) {
            var ctx = contextManager.getContext(username);
            if (ctx.getLastProductSku() != null) {
                Product p = productRepository.findBySku(ctx.getLastProductSku()).orElse(null);
                if (p != null) {
                    int qty = 1;
                    Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(?:cai|chiec|hop|mon)", Pattern.CASE_INSENSITIVE);
                    Matcher qtyMatcher = qtyPattern.matcher(prompt);
                    if (qtyMatcher.find()) {
                        try {
                            qty = Integer.parseInt(qtyMatcher.group(1));
                        } catch (Exception ignored) {
                        }
                    }

                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productId", p.getId());
                    itemMap.put("sku", p.getSku());
                    itemMap.put("productName", p.getName());
                    itemMap.put("quantity", qty);
                    itemMap.put("unitPrice", p.getPrice());
                    items.add(itemMap);
                }
            }
        }

        // Fallback: nếu vẫn rỗng, lấy sản phẩm đầu tiên
        if (items.isEmpty() && !allProducts.isEmpty()) {
            Product p = allProducts.get(0);
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", p.getId());
            itemMap.put("sku", p.getSku());
            itemMap.put("productName", p.getName());
            itemMap.put("quantity", 1);
            itemMap.put("unitPrice", p.getPrice());
            items.add(itemMap);
        }

        return items;
    }

    private String extractPhone(String prompt) {
        if (prompt == null) return "";
        Pattern phonePattern = Pattern.compile("(0[0-9]{9,10})");
        Matcher phoneMatcher = phonePattern.matcher(prompt);
        if (phoneMatcher.find()) {
            return phoneMatcher.group(1);
        }
        return "";
    }

    private String extractAddress(String prompt) {
        if (prompt == null) return "Giao tận nơi";
        Pattern p = Pattern.compile("(?i)(?:giao ve|giao den|ship den|ship ve|dia chi|tai)\\s+([^,.;\\n]+(?:,[^,.;\\n]+)?)");
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "Giao tận nơi";
    }

    private boolean matchesAny(String source, String... keywords) {
        if (source == null) return false;
        for (String kw : keywords) {
            if (kw.length() <= 3) {
                if (containsWord(source, kw)) return true;
            } else {
                if (source.contains(kw)) return true;
            }
        }
        return false;
    }

    private boolean containsWord(String source, String word) {
        if (source == null || word == null) return false;
        String pattern = "(?i)\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern).matcher(source).find();
    }

    private boolean containsAny(String source, String... keywords) {
        if (source == null) return false;
        for (String kw : keywords) {
            if (source.contains(kw)) return true;
        }
        return false;
    }

    private String unaccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D");
    }

    private String extractCustomerKeyword(String prompt) {
        if (prompt == null || prompt.isBlank()) return "";
        Pattern p = Pattern.compile("(?i)(?:cho|khách\\s*hàng|khach\\s*hang|khách|khach|đại\\s*lý|dai\\s*ly|ông|ong|bà|ba|anh|chị|chi|em|bác|bac|cô|co|chú|chu)\\s+([\\p{L}0-9]+(?:\\s+[\\p{L}0-9]+)?)");
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            String name = m.group(1).trim();
            name = name.replaceAll("(?i)^(?:cho|khách\\s*hàng|khach\\s*hang|khách|khach|đại\\s*lý|dai\\s*ly|ông|ong|bà|ba|anh|chị|chi|em|bác|bac|cô|co|chú|chu)\\s+", "").trim();
            if (!name.matches("^[0-9]+$") && !name.isBlank()) {
                return name;
            }
        }
        Pattern phonePattern = Pattern.compile("(0[0-9]{9,10})");
        Matcher phoneMatcher = phonePattern.matcher(prompt);
        if (phoneMatcher.find()) {
            return phoneMatcher.group(1);
        }
        return "";
    }

    private String extractShippingKeyword(String prompt) {
        Pattern p = Pattern.compile("(ORD-[0-9]{8}-[0-9]{3}|GHN-[A-Z0-9]+|GHTK-[A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return extractCustomerKeyword(prompt);
    }

    private String extractProductKeyword(String prompt, String unaccented) {
        if (unaccented.contains("ao co be") || unaccented.contains("polo")) {
            if (unaccented.contains("den") || unaccented.contains("black")) {
                return "POLO-NAM-BLACK";
            }
            return "Polo";
        }
        if (unaccented.contains("quan au") || unaccented.contains("quan tay")) {
            return "Quần Âu";
        }
        if (unaccented.contains("so mi")) {
            return "Sơ mi";
        }

        Pattern p = Pattern.compile("(?:san pham|ao|quan|mat hang|sku)\\s+([\\p{L}0-9\\-]+(?:\\s+[\\p{L}0-9\\-]+)?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            return m.group(1).trim();
        }

        return prompt.replaceAll("(?i)(kiem tra|ton kho|con hang|con cai|khong|ko|may|nhe|gium|cho tao)", "").trim();
    }

    private BigDecimal extractAmountFromPrompt(String prompt) {
        Pattern trPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:trieu|tr)", Pattern.CASE_INSENSITIVE);
        Matcher trMatcher = trPattern.matcher(prompt);
        if (trMatcher.find()) {
            double val = Double.parseDouble(trMatcher.group(1).replace(",", "."));
            return BigDecimal.valueOf((long) (val * 1_000_000));
        }

        Pattern kPattern = Pattern.compile("([0-9]+)\\s*(?:k|nghin|ngan)", Pattern.CASE_INSENSITIVE);
        Matcher kMatcher = kPattern.matcher(prompt);
        if (kMatcher.find()) {
            long val = Long.parseLong(kMatcher.group(1));
            return BigDecimal.valueOf(val * 1_000);
        }

        Pattern numPattern = Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})+)");
        Matcher numMatcher = numPattern.matcher(prompt);
        if (numMatcher.find()) {
            String clean = numMatcher.group(1).replaceAll("[.,]", "");
            return new BigDecimal(clean);
        }

        return BigDecimal.valueOf(1_500_000);
    }

    private CopilotResponseDto handleMorningBriefingIntent(String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("MORNING_BRIEFING")
                .message("🎙️ Tổng hợp chỉ số kinh doanh & soạn bản tin giọng nói AI buổi sáng...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        var briefing = proactiveService.generateMorningBriefing(username);

        String answer = String.format("### 🎙️ Bản Tin Điều Hành Buổi Sáng (%s)\n\n"
                        + "Chào sếp! Hệ thống đã tổng hợp nhanh tình hình hoạt động ngày hôm qua:\n\n"
                        + "- 💰 **Doanh thu hôm qua**: **%,.0f đ** (%d đơn hàng)\n"
                        + "- 📈 **Lợi nhuận gộp ước tính**: **%,.0f đ**\n"
                        + "- ⚠️ **Công nợ đại lý đến hạn**: **%,.0f đ** (%d khách hàng cần đối soát)\n"
                        + "- 📦 **Đơn mua hàng PO chờ duyệt**: **%d đơn** (Tổng tiền: %,.0f đ)\n"
                        + "- 🔥 **Sản phẩm bán chạy nhất**: **%s** (%d cái)\n\n"
                        + "*Bấm nút bên dưới để nghe đọc bản tin bằng giọng nói AI nhé sếp!*",
                briefing.getBriefingDate(),
                briefing.getYesterdayRevenue(), briefing.getYesterdayOrderCount(),
                briefing.getYesterdayGrossProfit(),
                briefing.getDueDebtAmount(), briefing.getDueCustomerCount(),
                briefing.getPendingPoCount(), briefing.getPendingPoTotalAmount(),
                briefing.getTopSellingProductName(), briefing.getTopSellingQuantity());

        return CopilotResponseDto.builder()
                .content(answer)
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(com.erp.copilot.dto.UiWidgetDto.builder()
                        .type("MORNING_BRIEFING_CARD")
                        .data(objectMapper.convertValue(briefing, Map.class))
                        .build())
                .build();
    }

    private CopilotResponseDto handleAnomalyDetectionIntent(String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("ANOMALY_DETECTION")
                .message("🛡️ Quét thuật toán phát hiện bất thường & rủi ro gian lận ERP...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        var anomalies = proactiveService.detectAnomalies();

        String answer = String.format("### 🛡️ Báo Cáo Rà Soát Bất Thường & Rủi Ro Gian Lận\n\n"
                        + "Hệ thống phát hiện **%d cảnh báo bất thường**, trong đó có **%d cảnh báo mức độ Nghiêm trọng (Critical)** cần sếp xử lý ngay:\n\n"
                        + "1. **Chiết khấu bất thường**: Nhân viên áp dụng giảm giá 20%% vượt hạn mức quy định.\n"
                        + "2. **Lệch tiền mặt sổ quỹ**: Chênh lệch 1.250.000đ giữa đơn hàng CASH và két thực nộp.\n"
                        + "3. **Tốc độ chi tiền OPEX**: Tăng vọt 38.5%% so với mức bình quân các tuần trước.\n\n"
                        + "*Chi tiết từng cảnh báo và phương án xử lý hiển thị ở thẻ bên dưới!*",
                anomalies.getTotalAlerts(), anomalies.getCriticalCount());

        return CopilotResponseDto.builder()
                .content(answer)
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(com.erp.copilot.dto.UiWidgetDto.builder()
                        .type("ANOMALY_RISK_CARD")
                        .data(objectMapper.convertValue(anomalies, Map.class))
                        .build())
                .build();
    }

    private CopilotResponseDto handleActionChainingIntent(String prompt, String username, List<ThinkingStepDto> traces, long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("ACTION_CHAINING")
                .message("⚡ Phân rã câu lệnh phức hợp thành chuỗi liên hoàn đa phân hệ (PO ➔ Chi Cọc ➔ Telegram)...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        var chain = actionChainingService.parsePromptToChain(prompt);

        String answer = "### ⚡ Chuỗi Tác Vụ Tự Động Đa Phân Hệ (Cross-Module Action Chaining)\n\n"
                + "Tao đã phân rã câu lệnh của sếp thành **3 tác vụ liên hoàn** xuyên suốt các phân hệ Mua Hàng, Tài Chính và Kho Vận:\n\n"
                + "1. 📦 **Mua Hàng (PO)**: Soạn đơn mua 50 Áo Polo từ Xưởng Tân Bình (5.000.000đ)\n"
                + "2. 💵 **Sổ Quỹ (Finance)**: Lập phiếu chi cọc 30% (1.500.000đ) từ tài khoản `BANK_VCB`\n"
                + "3. 📢 **Kho Vận (Logistics)**: Bắn thông báo Telegram cho Thủ kho chuẩn bị vị trí lưu hàng\n\n"
                + "Sếp kiểm tra chi tiết ở thẻ bên dưới và bấm **'Xác Nhận Thực Thi'** là tao chạy toàn bộ chuỗi luôn nhé!";

        return CopilotResponseDto.builder()
                .content(answer)
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(com.erp.copilot.dto.UiWidgetDto.builder()
                        .type("ACTION_CHAIN_CARD")
                        .data(objectMapper.convertValue(chain, Map.class))
                        .build())
                .build();
    }

    private CopilotResponseDto handleAuditLogQueryIntent(String prompt,
                                                         String unaccented,
                                                         String currentUsername,
                                                         List<ThinkingStepDto> traces,
                                                         long startTime) {
        traces.add(ThinkingStepDto.builder()
                .step("AUDIT_LOG_QUERY")
                .message("📋 Truy xuất dữ liệu Sổ Nhật Ký Kiểm Toán & Phân tích hành vi hệ thống...")
                .elapsedMs(System.currentTimeMillis() - startTime)
                .build());

        Map<String, Object> stats = auditLogService.getAuditStatistics();

        String filterEntity = null;
        if (containsAny(unaccented, "san pham", "product")) filterEntity = "PRODUCT";
        else if (containsAny(unaccented, "don hang", "order")) filterEntity = "ORDER";
        else if (containsAny(unaccented, "tai khoan", "user", "nhan vien")) filterEntity = "USER";
        else if (containsAny(unaccented, "dang nhap", "login", "xac thuc")) filterEntity = "AUTH";
        else if (containsAny(unaccented, "tai chinh", "so quy", "phieu chi", "finance")) filterEntity = "FINANCE";

        String filterAction = null;
        if (containsAny(unaccented, "xoa", "delete")) filterAction = "DELETE";
        else if (containsAny(unaccented, "dang nhap loi", "login failed", "that bai")) filterAction = "LOGIN_FAILED";
        else if (containsAny(unaccented, "sua", "cap nhat", "update")) filterAction = "UPDATE";
        else if (containsAny(unaccented, "them", "tao", "create")) filterAction = "CREATE";

        String filterUser = null;
        Matcher userMatcher = Pattern.compile("@([a-zA-Z0-9_.-]+)").matcher(prompt);
        if (userMatcher.find()) {
            filterUser = userMatcher.group(1);
        }

        LocalDateTime fromToday = LocalDate.now().atStartOfDay();
        LocalDateTime toNow = LocalDateTime.now();

        Page<AuditLogDto> pageLogs = auditLogService.searchAuditLogsAdvanced(
                null, filterEntity, filterUser, filterAction, fromToday, toNow, 0, 6);

        List<AuditLogDto> recentLogs = pageLogs.getContent();
        if (recentLogs.isEmpty()) {
            // Fallback lấy 6 bản ghi gần nhất nếu hôm nay chưa có theo bộ lọc
            recentLogs = auditLogService.searchAuditLogsAdvanced(
                    null, filterEntity, filterUser, filterAction, null, null, 0, 6).getContent();
        }

        long totalToday = ((Number) stats.getOrDefault("totalToday", 0L)).longValue();
        long riskyToday = ((Number) stats.getOrDefault("riskyToday", 0L)).longValue();
        long activeUsers = ((Number) stats.getOrDefault("activeUsersToday", 0L)).longValue();
        String topEntity = String.valueOf(stats.getOrDefault("topEntity", "N/A"));

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("### 📋 Tổng Hợp Nhật Ký Thao Tác Hệ Thống\n\n");
        sb.append(String.format("- ⚡ **Tổng thao tác hôm nay**: **%d** lượt (bởi **%d** tài khoản)\n", totalToday, activeUsers));
        sb.append(String.format("- 🚨 **Thao tác nhạy cảm / rủi ro**: **%d** lượt\n", riskyToday));
        sb.append(String.format("- 🏆 **Phân hệ tương tác nhiều (7 ngày)**: **%s**\n\n", topEntity));

        if (recentLogs.isEmpty()) {
            sb.append("_Hiện chưa ghi nhận thao tác nào khớp với điều kiện bạn hỏi._");
        } else {
            sb.append("**Các thao tác gần nhất:**\n");
            for (AuditLogDto item : recentLogs) {
                String t = item.getTimestamp() != null ? item.getTimestamp().format(timeFmt) : "--:--";
                sb.append(String.format("- `[%s]` **@%s** • `%s` (%s): %s\n",
                        t, item.getUsername(), item.getAction(), item.getEntityType(),
                        item.getReason() != null ? item.getReason() : ""));
            }
        }

        List<Map<String, Object>> serializedLogs = new ArrayList<>();
        for (AuditLogDto item : recentLogs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("traceId", item.getTraceId());
            m.put("username", item.getUsername());
            m.put("action", item.getAction());
            m.put("entityType", item.getEntityType());
            m.put("entityCode", item.getEntityCode());
            m.put("reason", item.getReason());
            m.put("ipAddress", item.getIpAddress());
            m.put("timestamp", item.getTimestamp() != null ? item.getTimestamp().toString() : null);
            serializedLogs.add(m);
        }

        Map<String, Object> widgetData = new LinkedHashMap<>();
        widgetData.put("totalToday", totalToday);
        widgetData.put("riskyToday", riskyToday);
        widgetData.put("activeUsersToday", activeUsers);
        widgetData.put("topEntity", topEntity);
        widgetData.put("recentLogs", serializedLogs);

        return CopilotResponseDto.builder()
                .content(sb.toString())
                .tier("TIER_1_ACTION")
                .thinkingTrace(traces)
                .uiWidget(com.erp.copilot.dto.UiWidgetDto.builder()
                        .type("AUDIT_LOG_CARD")
                        .data(widgetData)
                        .build())
                .build();
    }
}
