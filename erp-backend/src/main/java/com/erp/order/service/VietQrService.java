package com.erp.order.service;

import com.erp.order.dto.PaymentWebhookRequest;
import com.erp.order.dto.VietQrPaymentDto;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentMethod;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQrService {

    public static final String DEFAULT_BANK_ID = "MB";
    public static final String DEFAULT_BANK_NAME = "Ngân hàng TMCP Quân Đội (MBBank)";
    public static final String DEFAULT_ACCOUNT_NO = "0987654321";
    public static final String DEFAULT_ACCOUNT_NAME = "CONG TY ERP CLOUD ENTERPRISE";

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sinh thông tin mã VietQR động chuẩn NAPAS cho một đơn hàng
     */
    @Transactional(readOnly = true)
    public VietQrPaymentDto generateVietQrForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        return buildVietQrDto(order);
    }

    public VietQrPaymentDto buildVietQrDto(Order order) {
        long amountLong = order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0L;
        String transferContent = "DH " + order.getOrderCode();

        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(DEFAULT_ACCOUNT_NAME, StandardCharsets.UTF_8);

        // URL chuẩn VietQR QuickLink (template compact2 hiển thị đầy đủ thông tin hoá đơn & NAPAS247)
        String qrImageUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                DEFAULT_BANK_ID,
                DEFAULT_ACCOUNT_NO,
                amountLong,
                encodedContent,
                encodedAccountName);

        boolean isPaid = order.getPaymentStatus() == PaymentStatus.PAID;

        return VietQrPaymentDto.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .totalAmount(order.getTotalAmount())
                .bankId(DEFAULT_BANK_ID)
                .bankName(DEFAULT_BANK_NAME)
                .accountNo(DEFAULT_ACCOUNT_NO)
                .accountName(DEFAULT_ACCOUNT_NAME)
                .transferContent(transferContent)
                .qrImageUrl(qrImageUrl)
                .paid(isPaid)
                .build();
    }

    /**
     * Xử lý xác nhận thanh toán giả lập hoặc trực tiếp từ Webhook
     */
    @Transactional
    public Order processPaymentSuccess(Long orderId, String transactionId, String paymentChannel) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Đơn hàng [{}] đã thanh toán trước đó, bỏ qua xử lý lặp lại.", order.getOrderCode());
            return order;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(PaymentMethod.VIETQR);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(java.time.LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);
        log.info("Xác nhận thanh toán VietQR thành công cho đơn hàng: [{}] (Tổng: {} đ, Giao dịch: {})",
                saved.getOrderCode(), saved.getTotalAmount(), transactionId);

        // Broadcast sự kiện thanh toán qua WebSocket STOMP
        broadcastPaymentEvent(saved, transactionId, paymentChannel);

        return saved;
    }

    /**
     * Nhận và xử lý Webhook tự động từ cổng thanh toán ngân hàng
     */
    @Transactional
    public Map<String, Object> handlePaymentWebhook(PaymentWebhookRequest webhook) {
        String code = webhook.getOrderCode();
        if (code == null || code.trim().isEmpty()) {
            // Trích xuất mã đơn từ nội dung chuyển khoản
            if (webhook.getContent() != null) {
                Matcher m = Pattern.compile("(?i)\\b(ORD-[0-9A-Za-z_-]+)\\b").matcher(webhook.getContent());
                if (m.find()) {
                    code = m.group(1).toUpperCase();
                }
            }
        }

        if (code == null || code.trim().isEmpty()) {
            return Map.of("success", false, "message", "Không tìm thấy mã đơn hàng trong nội dung chuyển khoản!");
        }

        Optional<Order> orderOpt = orderRepository.findByOrderCode(code);
        if (orderOpt.isEmpty()) {
            return Map.of("success", false, "message", "Không tìm thấy đơn hàng: " + code);
        }

        Order order = orderOpt.get();
        processPaymentSuccess(order.getId(), webhook.getTransactionId(), webhook.getGateway());

        return Map.of(
                "success", true,
                "orderCode", order.getOrderCode(),
                "status", "PAID",
                "message", "Thanh toán thành công qua VietQR Webhook!"
        );
    }

    private void broadcastPaymentEvent(Order order, String transactionId, String paymentChannel) {
        try {
            Map<String, Object> event = Map.of(
                    "type", "PAYMENT_SUCCESS",
                    "orderId", order.getId(),
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "paymentStatus", "PAID",
                    "paymentMethod", "VIETQR",
                    "channel", paymentChannel != null ? paymentChannel : "VIETQR_NAPAS",
                    "transactionId", transactionId != null ? transactionId : "TXN-" + System.currentTimeMillis()
            );

            // Gửi tới kênh đơn hàng và phòng chat công khai
            messagingTemplate.convertAndSend("/topic/orders", (Object) event);
            messagingTemplate.convertAndSend("/topic/room.GLOBAL", (Object) Map.of(
                    "type", "CHAT",
                    "senderUsername", "system",
                    "senderFullName", "💳 Cổng Thanh Toán ERP",
                    "content", String.format("🎉 Đơn hàng **%s** (Trị giá %,.0f đ) vừa được khách hàng thanh toán thành công qua **VietQR (NAPAS 24/7)**!",
                            order.getOrderCode(), order.getTotalAmount().doubleValue())
            ));
        } catch (Exception ex) {
            log.warn("Không thể broadcast sự kiện thanh toán qua WebSocket: {}", ex.getMessage());
        }
    }
}
