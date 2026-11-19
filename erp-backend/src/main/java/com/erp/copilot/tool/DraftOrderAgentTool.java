package com.erp.copilot.tool;

import com.erp.copilot.dto.DraftOrderDataDto;
import com.erp.copilot.dto.DraftOrderDataDto.DraftOrderItemDto;
import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.copilot.service.CopilotContextManager;
import com.erp.model.Product;
import com.erp.order.dto.CreateOrderRequest;
import com.erp.order.dto.OrderItemRequest;
import com.erp.order.model.PaymentMethod;
import com.erp.order.model.PaymentStatus;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftOrderAgentTool implements AgentTool {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CopilotContextManager contextManager;
    private final ObjectMapper objectMapper;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "draftPosOrder";
    }

    @Override
    public String getDescription() {
        return "Soạn thảo Đơn hàng Bán lẻ/Sỉ (Conversational POS Draft) từ câu lệnh tự nhiên, kiểm tra tồn kho & sinh Thẻ Xác Nhận ORDER_DRAFT_CARD.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "customerName", Map.of("type", "string", "description", "Tên khách hàng"),
                        "customerPhone", Map.of("type", "string", "description", "Số điện thoại khách hàng"),
                        "shippingAddress", Map.of("type", "string", "description", "Địa chỉ giao hàng"),
                        "paymentMethod", Map.of("type", "string", "enum", List.of("CASH", "BANK_TRANSFER", "COD")),
                        "items", Map.of("type", "array", "description", "Danh sách sản phẩm và số lượng")
                ),
                "required", List.of("customerName", "items")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String customerName = (String) args.getOrDefault("customerName", "Khách lẻ tại quầy");
        String customerPhone = (String) args.getOrDefault("customerPhone", "");
        String shippingAddress = (String) args.getOrDefault("shippingAddress", "Tại cửa hàng / Kho");
        String paymentMethodStr = (String) args.getOrDefault("paymentMethod", "CASH");
        String note = (String) args.getOrDefault("note", "Lên đơn nhanh qua Copilot Ultra VIP");

        PaymentMethod paymentMethod = PaymentMethod.CASH;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) args.getOrDefault("items", Collections.emptyList());
        List<DraftOrderItemDto> draftItems = new ArrayList<>();
        List<OrderItemRequest> orderItemRequests = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allInStock = true;

        for (Map<String, Object> it : rawItems) {
            Long productId = it.get("productId") != null ? Long.valueOf(it.get("productId").toString()) : null;
            String sku = (String) it.get("sku");
            Integer quantity = it.get("quantity") != null ? Integer.valueOf(it.get("quantity").toString()) : 1;
            if (quantity <= 0) quantity = 1;

            Product product = null;
            if (productId != null) {
                product = productRepository.findById(productId).orElse(null);
            }
            if (product == null && sku != null && !sku.isBlank()) {
                product = productRepository.findBySku(sku).orElse(null);
            }

            if (product != null) {
                BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                subtotal = subtotal.add(lineTotal);

                int physicalStock = product.getPhysicalStock() != null ? product.getPhysicalStock() : (product.getStock() != null ? product.getStock() : 0);
                int reserved = product.getReservedStock() != null ? product.getReservedStock() : 0;
                int available = Math.max(0, physicalStock - reserved);
                boolean isSufficient = available >= quantity;
                if (!isSufficient) {
                    allInStock = false;
                }

                draftItems.add(DraftOrderItemDto.builder()
                        .productId(product.getId())
                        .sku(product.getSku())
                        .productName(product.getName())
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .lineTotal(lineTotal)
                        .stockAvailable(available)
                        .isSufficient(isSufficient)
                        .build());

                orderItemRequests.add(OrderItemRequest.builder()
                        .productId(product.getId())
                        .quantity(quantity)
                        .uomName(product.getBaseUnit() != null ? product.getBaseUnit() : "Cái")
                        .uomConversionRate(1)
                        .unitPrice(unitPrice)
                        .build());

                // Cập nhật context sản phẩm gần nhất
                contextManager.updateProduct(currentUsername, product.getId(), product.getSku(), product.getName(), unitPrice, physicalStock);
            }
        }

        BigDecimal taxRate = BigDecimal.valueOf(0.10); // Thuế VAT 10%
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount).subtract(discountAmount);

        // Lưu đơn hàng dự thảo vào Session Context của User
        CreateOrderRequest draftOrderRequest = new CreateOrderRequest();
        draftOrderRequest.setCustomerName(customerName);
        draftOrderRequest.setCustomerPhone(customerPhone);
        draftOrderRequest.setShippingAddress(shippingAddress);
        draftOrderRequest.setNote(note);
        draftOrderRequest.setPaymentMethod(paymentMethod);
        draftOrderRequest.setPaymentStatus(PaymentStatus.UNPAID);
        draftOrderRequest.setDiscountAmount(discountAmount);
        draftOrderRequest.setTaxRate(taxRate);
        draftOrderRequest.setItems(orderItemRequests);

        contextManager.setDraftOrder(currentUsername, draftOrderRequest);

        // Cập nhật context khách hàng
        customerRepository.findByPhone(customerPhone).ifPresent(c -> 
            contextManager.updateCustomer(currentUsername, c.getId(), c.getName(), c.getPhone(), c.getCurrentDebt())
        );

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);
        DraftOrderDataDto draftData = DraftOrderDataDto.builder()
                .customerName(customerName)
                .customerPhone(customerPhone)
                .shippingAddress(shippingAddress)
                .paymentMethod(paymentMethod.name())
                .note(note)
                .items(draftItems)
                .subtotal(subtotal)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .allInStock(allInStock)
                .warningMessage(!allInStock ? "⚠️ Có sản phẩm không đủ tồn kho khả dụng để giao ngay!" : null)
                .build();

        Map<String, Object> widgetData;
        try {
            widgetData = objectMapper.convertValue(draftData, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            widgetData = new HashMap<>();
        }

        UiWidgetDto uiWidget = UiWidgetDto.builder()
                .type("ORDER_DRAFT_CARD")
                .data(widgetData)
                .build();

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("📝 **Đã lập xong Đơn hàng Dự thảo cho: %s** 🌿\n\n", customerName));
        msg.append(String.format("- **Tổng giá trị dự kiến**: **%s** (gồm VAT 10%%)\n", currFmt.format(totalAmount)));
        msg.append(String.format("- **Phương thức thanh toán**: `%s` | **Địa chỉ**: *%s*\n", paymentMethod.name(), shippingAddress));
        if (allInStock) {
            msg.append("- **Tình trạng kho hàng**: ✅ **Đủ 100% hàng trong kho**, sẵn sàng giữ chỗ & xuất đơn!\n\n");
        } else {
            msg.append("- **Tình trạng kho hàng**: ⚠️ **Một số mặt hàng thiếu tồn kho**, cần kiểm tra trước khi xác nhận!\n\n");
        }
        msg.append("👉 Mày kiểm tra lại thông tin trên **Thẻ Đơn Hàng Dự Thảo** bên dưới, rồi bấm **[✅ Xác nhận Tạo Đơn]** để tao lưu hệ thống ngay nhé! 🚀");

        return ToolExecutionResult.builder()
                .toolName(getToolName())
                .success(true)
                .message(msg.toString())
                .data(widgetData)
                .uiWidget(uiWidget)
                .build();
    }
}
