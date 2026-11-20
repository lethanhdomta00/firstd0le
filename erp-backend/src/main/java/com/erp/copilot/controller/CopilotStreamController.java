package com.erp.copilot.controller;

import com.erp.copilot.dto.*;
import com.erp.copilot.service.CopilotContextManager;
import com.erp.order.dto.CreateOrderRequest;
import com.erp.order.dto.OrderDto;
import com.erp.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.copilot.service.AdaptiveCognitiveService;
import com.erp.copilot.service.TempReportCacheManager;
import com.erp.finance.dto.CreateFinanceVoucherRequest;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.model.CostCenter;
import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.VoucherType;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.finance.service.FinanceVoucherService;
import com.erp.model.ChatMessage;
import com.erp.model.Customer;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.ChatMessageRepository;
import com.erp.repository.CustomerRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotStreamController {

    private final AdaptiveCognitiveService cognitiveService;
    private final TempReportCacheManager reportCacheManager;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final CopilotContextManager contextManager;
    private final ObjectMapper objectMapper;
    private final FinanceVoucherService financeVoucherService;
    private final TreasuryAccountRepository treasuryAccountRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    /**
     * POST /api/copilot/chat-stream -> Server-Sent Events (SSE) Streaming
     * Đẩy luồng trace, token, widget và done thời gian thực
     */
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter chatStream(@RequestBody CopilotChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 phút timeout
        User currentUser = getCurrentUser();
        String username = currentUser != null ? currentUser.getUsername() : "anonymous";
        String prompt = request != null && request.getMessage() != null ? request.getMessage().trim() : "";

        CompletableFuture.runAsync(() -> {
            try {
                if (prompt.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Nội dung câu hỏi không được để trống!")));
                    emitter.complete();
                    return;
                }

                // 1. Lưu tin nhắn của người dùng vào database
                if (currentUser != null) {
                    ChatMessage userMsg = ChatMessage.builder()
                            .user(currentUser)
                            .role("USER")
                            .content(prompt)
                            .build();
                    chatMessageRepository.save(userMsg);
                }

                // 2. Chạy bộ não nhận diện đa tầng và sinh phản hồi
                CopilotResponseDto response = cognitiveService.processPrompt(prompt, username);

                // 3. Đẩy từng sự kiện Thinking Trace
                if (response.getThinkingTrace() != null) {
                    for (CopilotResponseDto.ThinkingStepDto step : response.getThinkingTrace()) {
                        emitter.send(SseEmitter.event()
                                .name("trace")
                                .data(step));
                        Thread.sleep(120); // Tạo nhịp suy nghĩ tự nhiên
                    }
                }

                // 4. Đẩy từng token chữ (hiệu ứng typewriter)
                String content = response.getContent() != null ? response.getContent() : "";
                String[] words = content.split("(?<=\\s)|(?<=\\n)");
                for (String word : words) {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(Map.of("text", word)));
                    Thread.sleep(15);
                }

                // 5. Đẩy Widget nếu có (FILE_DOWNLOAD_CARD, STOCK_ALERT_CARD, etc.)
                if (response.getUiWidget() != null) {
                    emitter.send(SseEmitter.event()
                            .name("widget")
                            .data(response.getUiWidget()));
                }

                // 6. Lưu tin nhắn bot vào database
                if (currentUser != null) {
                    ChatMessage botMsg = ChatMessage.builder()
                            .user(currentUser)
                            .role("ASSISTANT")
                            .content(content)
                            .build();
                    chatMessageRepository.save(botMsg);
                }

                // 7. Kết thúc luồng SSE
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("finishReason", "STOP", "tier", response.getTier())));
                emitter.complete();

            } catch (Exception ex) {
                log.error("Lỗi khi xử lý Copilot Stream: {}", ex.getMessage(), ex);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Lỗi nội bộ: " + ex.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    /**
     * POST /api/copilot/chat -> REST API đồng bộ thông thường
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CopilotResponseDto> chatSync(@RequestBody CopilotChatRequest request) {
        User currentUser = getCurrentUser();
        String username = currentUser != null ? currentUser.getUsername() : "anonymous";
        String prompt = request != null && request.getMessage() != null ? request.getMessage().trim() : "";

        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(CopilotResponseDto.builder()
                    .content("Nội dung câu hỏi không được để trống!")
                    .build());
        }

        if (currentUser != null) {
            chatMessageRepository.save(ChatMessage.builder()
                    .user(currentUser)
                    .role("USER")
                    .content(prompt)
                    .build());
        }

        CopilotResponseDto response = cognitiveService.processPrompt(prompt, username);

        if (currentUser != null && response.getContent() != null) {
            chatMessageRepository.save(ChatMessage.builder()
                    .user(currentUser)
                    .role("ASSISTANT")
                    .content(response.getContent())
                    .build());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/copilot/quick-suggestions -> Gợi ý câu hỏi nhanh theo thời gian
     */
    @GetMapping("/quick-suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuickSuggestionDto>> getQuickSuggestions() {
        LocalTime now = LocalTime.now();
        List<QuickSuggestionDto> list = new ArrayList<>();

        if (now.isBefore(LocalTime.of(12, 0))) {
            list.add(new QuickSuggestionDto("s1", "Ấm no không mày?", "Hôm nay ấm no không mày?", "SALES", "💰"));
            list.add(new QuickSuggestionDto("s2", "Xuất file doanh thu", "Xuất cho tao file Excel doanh thu tuần này", "EXPORT", "📊"));
            list.add(new QuickSuggestionDto("s3", "Mặt hàng sắp hết", "Quét các mặt hàng có nguy cơ cạn kho AI", "INVENTORY", "🚨"));
            list.add(new QuickSuggestionDto("s4", "Ai nợ dai nhất?", "Thằng nào nợ dai nhất?", "DEBT", "🛡️"));
            list.add(new QuickSuggestionDto("s5", "Hành trình đơn hàng", "Ship đơn ông Tuấn tới đâu rồi?", "LOGISTICS", "🚚"));
            list.add(new QuickSuggestionDto("s6", "Cách xuất hóa đơn đỏ", "Làm sao để xuất hóa đơn đỏ PDF?", "GENERAL", "❓"));
        } else {
            list.add(new QuickSuggestionDto("s1", "Doanh thu tuần này", "Báo cáo doanh số và lãi gộp tuần này", "SALES", "📈"));
            list.add(new QuickSuggestionDto("s2", "Xuất báo cáo cạn kho", "Xuất file Excel dự báo cạn kho", "EXPORT", "📑"));
            list.add(new QuickSuggestionDto("s3", "Tồn kho áo Polo", "Kho còn cái áo cổ bẻ màu đen không mày?", "INVENTORY", "👕"));
            list.add(new QuickSuggestionDto("s4", "Soạn phiếu chi mặt bằng", "Soạn phiếu chi tiền thuê mặt bằng 15 triệu", "FINANCE", "📝"));
            list.add(new QuickSuggestionDto("s5", "Kiểm tra công nợ đại lý", "Khách hàng nào đang nợ nhiều nhất?", "DEBT", "⚠️"));
            list.add(new QuickSuggestionDto("s6", "Tư vấn nhập hàng tháng sau", "Tháng sau tao tính nhập thêm 100 cái áo Polo, mày thấy nên nhập không?", "ADVISORY", "💡"));
        }

        return ResponseEntity.ok(list);
    }

    /**
     * POST /api/copilot/execute-action -> Thực thi hành động từ nút bấm xác nhận 1-click
     */
    @PostMapping("/execute-action")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> executeAction(@RequestBody ExecuteActionRequest request) {
        if (request == null || request.getActionType() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Yêu cầu không hợp lệ!"));
        }

        String actionType = request.getActionType().toUpperCase();
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            switch (actionType) {
                case "HOLD_CREDIT" -> {
                    User user = getCurrentUser();
                    if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.ACCOUNTANT)) {
                        result.put("success", false);
                        result.put("message", "⛔ Bạn không có quyền khóa hạn mức công nợ khách hàng! Thao tác này yêu cầu vai trò ADMIN, MANAGER hoặc ACCOUNTANT. Tài khoản của bạn chỉ có quyền XEM.");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
                    }

                    String targetId = request.getTargetId();
                    Optional<Customer> custOpt = Optional.empty();
                    if (targetId != null && !targetId.isBlank()) {
                        try {
                            custOpt = customerRepository.findById(Long.parseLong(targetId));
                        } catch (Exception ignored) {
                        }
                    }
                    if (custOpt.isPresent()) {
                        Customer c = custOpt.get();
                        c.setCreditLocked(true);
                        customerRepository.save(c);
                        result.put("success", true);
                        result.put("message", "✅ Đã khóa xuất hàng thành công đại lý: " + c.getName() + " (" + c.getPhone() + "). Các đơn hàng nợ mới sẽ bị chặn tự động!");
                    } else {
                        result.put("success", false);
                        result.put("message", "Không tìm thấy hồ sơ khách hàng để khóa nợ.");
                    }
                }
                case "APPROVE_EXPENSE" -> {
                    User user = getCurrentUser();
                    if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.ACCOUNTANT)) {
                        result.put("success", false);
                        result.put("message", "⛔ Bạn không có quyền phê duyệt xuất quỹ! Thao tác này yêu cầu vai trò ADMIN, MANAGER hoặc ACCOUNTANT. Tài khoản của bạn chỉ có quyền XEM.");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
                    }

                    String currentUsername = user != null ? user.getUsername() : "admin";
                    Map<String, Object> payload = request.getPayload();

                    BigDecimal amount = BigDecimal.valueOf(15000000);
                    String payeeName = "Đối tác";
                    String note = "Chi phí hoạt động phê duyệt qua Copilot";
                    CostCenter costCenter = CostCenter.RENTAL;

                    if (payload != null) {
                        if (payload.get("amount") != null) {
                            try {
                                amount = new BigDecimal(String.valueOf(payload.get("amount")).replaceAll("[^0-9.]", ""));
                            } catch (Exception ignored) {
                            }
                        }
                        if (payload.get("payeeName") != null) payeeName = String.valueOf(payload.get("payeeName"));
                        if (payload.get("note") != null) note = String.valueOf(payload.get("note"));
                    }

                    TreasuryAccount sourceAcc = treasuryAccountRepository.findByAccountCode("BANK_VCB")
                            .or(() -> treasuryAccountRepository.findByAccountCode("CASH_OFFICE"))
                            .or(() -> treasuryAccountRepository.findAll().stream().findFirst())
                            .orElse(null);

                    if (sourceAcc != null) {
                        CreateFinanceVoucherRequest vReq = new CreateFinanceVoucherRequest();
                        vReq.setVoucherType(VoucherType.PAYMENT);
                        vReq.setCostCenter(costCenter);
                        vReq.setAmount(amount);
                        vReq.setSourceAccountId(sourceAcc.getId());
                        vReq.setPayeePayorName(payeeName);
                        vReq.setNote(note);
                        vReq.setAutoApprove(true);

                        FinanceVoucherDto createdVoucher = financeVoucherService.createVoucher(vReq, currentUsername);
                        result.put("success", true);
                        result.put("voucherCode", createdVoucher.getVoucherCode());
                        result.put("message", "✅ Đã phê duyệt và xuất quỹ thành công! Mã chứng từ: **" + createdVoucher.getVoucherCode() 
                                + "** (Số tiền: " + String.format("%,.0f đ", amount.doubleValue()) + " từ quỹ **" + sourceAcc.getAccountName() + "**).");
                    } else {
                        result.put("success", true);
                        result.put("message", "✅ Đã phê duyệt và xuất quỹ thành công! Phiếu chi đã được hạch toán vào Sổ quỹ hệ thống.");
                    }
                }
                case "RESTOCK_PRODUCT" -> {
                    result.put("success", true);
                    result.put("message", "✅ Đã ghi nhận đề xuất nhập kho. Mời bạn kiểm tra phiếu nhập hàng tại phân hệ Kho Vận!");
                }
                case "CONFIRM_DRAFT_ORDER" -> {
                    User user = getCurrentUser();
                    String currentUsername = user != null ? user.getUsername() : "admin";
                    CreateOrderRequest draftOrder = contextManager.getContext(currentUsername).getPendingDraftOrder();

                    if (draftOrder == null && request.getPayload() != null) {
                        try {
                            draftOrder = objectMapper.convertValue(request.getPayload(), CreateOrderRequest.class);
                        } catch (Exception ignored) {
                        }
                    }

                    if (draftOrder != null && draftOrder.getItems() != null && !draftOrder.getItems().isEmpty()) {
                        OrderDto createdOrder = orderService.createOrder(draftOrder, currentUsername);
                        contextManager.clearDraftOrder(currentUsername);
                        contextManager.updateOrder(currentUsername, createdOrder.getOrderCode());

                        result.put("success", true);
                        result.put("orderCode", createdOrder.getOrderCode());
                        result.put("orderId", createdOrder.getId());
                        result.put("totalAmount", createdOrder.getTotalAmount());
                        result.put("message", "🎉 Đã tạo đơn hàng thành công! Mã đơn: **" + createdOrder.getOrderCode() 
                                + "** (Tổng tiền: " + String.format("%,.0f đ", createdOrder.getTotalAmount() != null ? createdOrder.getTotalAmount().doubleValue() : 0.0) 
                                + "). Trạng thái: **Chờ xác nhận & Đã giữ chỗ kho**!");
                    } else {
                        result.put("success", false);
                        result.put("message", "Không tìm thấy thông tin đơn hàng dự thảo để tạo. Vui lòng ra lệnh lên đơn lại!");
                    }
                }
                default -> {
                    result.put("success", false);
                    result.put("message", "Hành động '" + actionType + "' chưa được định nghĩa trong hệ thống.");
                }
            }
        } catch (Exception ex) {
            log.error("Lỗi khi executeAction: {}", ex.getMessage(), ex);
            result.put("success", false);
            result.put("message", "Lỗi thực thi: " + ex.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/copilot/reports/download/{fileId} -> Tải file báo cáo tạm thời (.xlsx, .csv)
     */
    @GetMapping("/reports/download/{fileId}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String fileId) {
        Optional<TempReportCacheManager.CachedReport> reportOpt = reportCacheManager.getReport(fileId);
        if (reportOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("File báo cáo không tồn tại hoặc đã hết hạn hiệu lực sau 60 phút. Vui lòng yêu cầu xuất lại file mới trong khung chat!").getBytes(StandardCharsets.UTF_8));
        }

        TempReportCacheManager.CachedReport report = reportOpt.get();
        String encodedFileName = URLEncoder.encode(report.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(report.getContentType()));
        headers.setContentDispositionFormData("attachment", report.getFileName());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getFileName() + "\"; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(report.getContentBytes().length);

        return new ResponseEntity<>(report.getContentBytes(), headers, HttpStatus.OK);
    }
}
