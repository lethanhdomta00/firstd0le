package com.erp.order;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.dto.CreateOrderRequest;
import com.erp.order.dto.OrderDto;
import com.erp.order.dto.OrderItemRequest;
import com.erp.order.dto.StockAdjustRequest;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.model.OrderStatus;
import com.erp.order.service.OrderService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MultiStateInventoryAndCostTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.erp.order.service.VietQrService vietQrService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Bàn phím cơ Cơ sở Pro");
        testProduct.setSku("KBD-PRO-" + System.nanoTime());
        testProduct.setBarcode("89385001" + (System.currentTimeMillis() % 100000));
        testProduct.setBaseUnit("Cái");
        testProduct.setPrice(new BigDecimal("150000"));
        testProduct.setWholesalePrice(new BigDecimal("130000"));
        testProduct.setVipPrice(new BigDecimal("120000"));
        testProduct.setMovingAvgCost(new BigDecimal("100000"));
        testProduct.setPhysicalStock(10);
        testProduct.setReservedStock(0);
        testProduct.setIncomingStock(0);
        testProduct.setStock(10);
        testProduct = productRepository.saveAndFlush(testProduct);
    }

    @Test
    @DisplayName("1. Kiểm tra Tồn kho 4 trạng thái & Giữ chỗ nguyên tử (Atomic Reservation) khi tạo đơn")
    void testAtomicStockReservationAndCostSnapshot() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Khách Hàng Lẻ");
        request.setCustomerPhone("0912345678");
        request.setShippingAddress("Hà Nội");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(4);
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = orderService.createOrder(request, "admin");

        assertNotNull(orderDto.getId());
        assertEquals(OrderStatus.PENDING, orderDto.getStatus());

        // Kiểm tra snapshot giá vốn & lợi nhuận gộp
        assertEquals(0, new BigDecimal("100000").compareTo(orderDto.getItems().get(0).getSnapshotCostPrice()));
        // Lãi gộp = (150.000 - 100.000) * 4 = 200.000đ
        assertEquals(0, new BigDecimal("200000").compareTo(orderDto.getGrossProfit()));

        // Kiểm tra tồn kho sau khi giữ chỗ (Reservation)
        Product reloaded = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, reloaded.getPhysicalStock(), "Hàng trên kệ chưa xuất nên physicalStock giữ nguyên 10");
        assertEquals(4, reloaded.getReservedStock(), "reservedStock tăng lên 4 chiếc để giữ chỗ");
        assertEquals(6, reloaded.getAvailableStock(), "Tồn khả dụng giảm còn 10 - 4 = 6");
        assertEquals(6, reloaded.getStock(), "Cột tương thích ngược stock phải bằng 6");
    }

    @Test
    @DisplayName("2. Kiểm tra Chống bán lố (Overselling Prevention) khi tồn khả dụng không đủ")
    void testOversellingPrevention() {
        // Đã có 10 chiếc trong kho, thử đặt ngay 11 chiếc
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Đại lý ôm hàng");
        request.setCustomerPhone("0988776655");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(11);
        request.setItems(List.of(itemRequest));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request, "admin");
        });

        assertTrue(ex.getMessage().contains("không đủ để giữ chỗ"));

        // Xác nhận tồn kho nguyên vẹn
        Product reloaded = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, reloaded.getPhysicalStock());
        assertEquals(0, reloaded.getReservedStock());
        assertEquals(10, reloaded.getAvailableStock());
    }

    @Test
    @DisplayName("3. Kiểm tra Xuất kho CONFIRMED: Trừ hàng thực tế (physicalStock) và giải phóng giữ chỗ")
    void testConfirmOrderDeductsPhysicalStock() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Khách Hàng VIP");
        request.setCustomerPhone("0909090909");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(3);
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = orderService.createOrder(request, "admin");

        // Chuyển sang CONFIRMED (Xuất kho)
        orderService.updateOrderStatus(orderDto.getId(), OrderStatus.CONFIRMED, "Đã đóng gói xuất kho", "admin");

        Product reloaded = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(7, reloaded.getPhysicalStock(), "Hàng thực tế trên kệ giảm từ 10 xuống 7");
        assertEquals(0, reloaded.getReservedStock(), "Giữ chỗ đã được giải tỏa hoàn toàn về 0");
        assertEquals(7, reloaded.getAvailableStock(), "Tồn khả dụng thực tế còn 7");
    }

    @Test
    @DisplayName("4. Kiểm tra Hủy đơn PENDING: Giải phóng giữ chỗ và phục hồi tồn khả dụng")
    void testCancelPendingOrderReleasesReservedStock() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Khách Hủy Nhanh");
        request.setCustomerPhone("0933221100");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(5);
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = orderService.createOrder(request, "admin");

        Product midProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(5, midProduct.getReservedStock());
        assertEquals(5, midProduct.getAvailableStock());

        // Hủy đơn PENDING
        orderService.updateOrderStatus(orderDto.getId(), OrderStatus.CANCELLED, "Khách đổi ý hủy đơn", "admin");

        Product restored = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, restored.getPhysicalStock());
        assertEquals(0, restored.getReservedStock(), "Giữ chỗ phải được hoàn trả về 0");
        assertEquals(10, restored.getAvailableStock(), "Tồn khả dụng phục hồi lại trọn vẹn 10");
    }

    @Test
    @DisplayName("5. Kiểm tra Giá vốn Bình quân gia quyền Moving Average Cost (MAC) khi nhập kho")
    void testMovingAverageCostCalculationOnInbound() {
        // testProduct có 10 chiếc @ 100.000đ = 1.000.000đ
        // Nhập thêm 10 chiếc @ 120.000đ = 1.200.000đ
        // MAC mới = (1.000.000 + 1.200.000) / (10 + 10) = 2.200.000 / 20 = 110.000đ
        StockAdjustRequest adjustRequest = new StockAdjustRequest();
        adjustRequest.setProductId(testProduct.getId());
        adjustRequest.setQuantityDelta(10);
        adjustRequest.setNote("Nhập hàng từ nhà máy mới");
        adjustRequest.setInboundCostPrice(new BigDecimal("120000"));

        orderService.adjustStock(adjustRequest, "admin");

        Product reloaded = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(20, reloaded.getPhysicalStock());
        assertEquals(20, reloaded.getAvailableStock());
        assertEquals(0, new BigDecimal("110000").compareTo(reloaded.getMovingAvgCost()),
                "Giá vốn bình quân di động mới phải là 110.000đ chuẩn xác");
    }

    @Test
    @DisplayName("6. Kiểm tra Credit Gatekeeper: Chặn xuất kho đơn vượt hạn mức nợ và yêu cầu phê duyệt")
    void testCreditGatekeeperWorkflow() {
        // Tạo khách hàng có hạn mức nợ 500.000đ
        String testPhone = "09" + String.format("%08d", Math.abs(System.nanoTime() % 100000000));
        Customer dealer = new Customer();
        dealer.setName("Đại lý Cấp 1 Minh Khai");
        dealer.setPhone(testPhone);
        dealer.setCreditLimit(new BigDecimal("500000"));
        dealer.setCurrentDebt(BigDecimal.ZERO);
        dealer.setCreditLocked(false);
        customerRepository.saveAndFlush(dealer);

        // Đặt đơn hàng 5 chiếc @ 150.000đ = 750.000đ (> 500.000đ)
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName(dealer.getName());
        request.setCustomerPhone(dealer.getPhone());

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(5);
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = orderService.createOrder(request, "admin");

        // Kiểm tra cờ vượt hạn mức tín dụng
        assertTrue(orderDto.isCreditExceeded(), "Đơn phải được đánh dấu vượt hạn mức nợ");
        assertEquals("PENDING", orderDto.getCreditApprovalStatus(), "Trạng thái duyệt nợ ban đầu là PENDING");

        // Khi chưa duyệt nợ mà cố tình xuất kho => Bị chặn
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatus(orderDto.getId(), OrderStatus.CONFIRMED, "Cố xuất kho", "admin");
        });
        assertTrue(ex.getMessage().contains("vượt hạn mức công nợ đại lý"));

        // Admin phê duyệt nợ
        OrderDto approvedOrder = orderService.approveCredit(orderDto.getId(), "admin", true);
        assertEquals("APPROVED", approvedOrder.getCreditApprovalStatus());

        // Sau khi đã phê duyệt nợ => Xuất kho thành công!
        OrderDto confirmedOrder = orderService.updateOrderStatus(orderDto.getId(), OrderStatus.CONFIRMED, "Duyệt xuất kho", "admin");
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getStatus());
    }

    @Test
    @DisplayName("7. Giai đoạn 2: Kiểm tra UoM Conversion Rate - Quy đổi đơn vị tính Thùng/Hộp sang Tồn cơ sở")
    void testUomConversionRateAndStockDeduction() {
        // testProduct có 10 Cái
        // Đặt mua 1 Thùng (hệ số quy đổi 5 cái / thùng)
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Đại lý Bia Sài Gòn");
        request.setCustomerPhone("0988776655");

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(testProduct.getId());
        itemRequest.setQuantity(1); // 1 Thùng
        itemRequest.setUomName("Thùng");
        itemRequest.setUomConversionRate(5); // 1 Thùng = 5 Cái
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = orderService.createOrder(request, "admin");

        // Sau khi tạo đơn: Tồn giữ chỗ phải tăng 5 Cái cơ sở, Tồn khả dụng còn 10 - 5 = 5 Cái
        Product prodAfterReserve = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(5, prodAfterReserve.getReservedStock(), "Reserved stock phải là 5 cái");
        assertEquals(5, prodAfterReserve.getAvailableStock(), "Available stock còn lại 5 cái");

        // Xuất kho: Physical stock phải giảm 5 cái, còn lại 10 - 5 = 5 cái
        orderService.updateOrderStatus(orderDto.getId(), OrderStatus.CONFIRMED, "Xuất kho 1 thùng", "admin");
        Product prodAfterDeduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(5, prodAfterDeduct.getPhysicalStock(), "Physical stock phải bị trừ 5 cái theo hệ số UoM");
        assertEquals(0, prodAfterDeduct.getReservedStock(), "Reserved stock giải phóng về 0");
    }

    @Test
    @DisplayName("8. Giai đoạn 2: Kiểm tra Pricing Resolution Engine - Bậc số lượng (Volume Tier) & Hạng khách (VIP)")
    void testPricingResolutionEngine() {
        // Cập nhật tồn kho lên 50 cái để test mua số lượng lớn
        testProduct.setPhysicalStock(50);
        testProduct.setStock(50);
        productRepository.saveAndFlush(testProduct);

        // Case 1: Khách lẻ mua 2 cái (< 10) => Đơn giá niêm yết 150.000đ
        CreateOrderRequest req1 = new CreateOrderRequest();
        req1.setCustomerName("Khách Mua Lẻ 1");
        req1.setCustomerPhone("0900000001");
        req1.setItems(List.of(OrderItemRequest.builder().productId(testProduct.getId()).quantity(2).build()));
        OrderDto order1 = orderService.createOrder(req1, "admin");
        assertEquals(0, new BigDecimal("300000.00").compareTo(order1.getSubtotal()), "2 cái x 150.000đ = 300.000đ");

        // Case 2: Khách lẻ mua 10 cái (Volume Tier >= 10) => Tự động áp giá sỉ 130.000đ
        CreateOrderRequest req2 = new CreateOrderRequest();
        req2.setCustomerName("Khách Mua Lớn");
        req2.setCustomerPhone("0900000002");
        req2.setItems(List.of(OrderItemRequest.builder().productId(testProduct.getId()).quantity(10).build()));
        OrderDto order2 = orderService.createOrder(req2, "admin");
        assertEquals(0, new BigDecimal("1300000.00").compareTo(order2.getSubtotal()), "10 cái x 130.000đ giá sỉ = 1.300.000đ");

        // Case 3: Khách hàng đạt hạng VIP mua 1 cái => Tự động áp giá VIP 120.000đ
        Customer vipCustomer = new Customer();
        vipCustomer.setName("Khách Hàng VIP Trâm Anh");
        vipCustomer.setPhone("0999888777");
        vipCustomer.setPriceTier("VIP");
        customerRepository.saveAndFlush(vipCustomer);

        CreateOrderRequest req3 = new CreateOrderRequest();
        req3.setCustomerName(vipCustomer.getName());
        req3.setCustomerPhone(vipCustomer.getPhone());
        req3.setItems(List.of(OrderItemRequest.builder().productId(testProduct.getId()).quantity(1).build()));
        OrderDto order3 = orderService.createOrder(req3, "admin");
        assertEquals(0, new BigDecimal("120000.00").compareTo(order3.getSubtotal()), "1 cái hạng VIP áp giá 120.000đ");
    }

    @Test
    @DisplayName("9. Giai đoạn 2: Báo cáo Tuổi nợ (Aging Debt) & Webhook Gạch nợ Idempotent chống trùng lặp")
    void testDebtAgingAndWebhookIdempotency() {
        String phone = "0933333333";
        Customer debtor = new Customer();
        debtor.setName("Đại lý Miền Trung");
        debtor.setPhone(phone);
        debtor.setCreditLimit(new BigDecimal("10000000"));
        debtor.setCurrentDebt(new BigDecimal("2000000"));
        customerRepository.saveAndFlush(debtor);

        // 1. Kiểm tra Báo cáo Tuổi nợ (Debt Aging Report)
        List<com.erp.order.dto.CustomerDebtAgingDto> agingReport = orderService.getDebtAgingReport(debtor.getName());
        assertFalse(agingReport.isEmpty(), "Báo cáo tuổi nợ phải tìm thấy khách hàng");
        com.erp.order.dto.CustomerDebtAgingDto row = agingReport.get(0);
        assertEquals(0, new BigDecimal("2000000").compareTo(row.getCurrentDebt()), "Dư nợ khớp 2.000.000đ");

        // 2. Tạo một đơn hàng nợ để test Webhook gạch nợ
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerName(debtor.getName());
        req.setCustomerPhone(phone);
        req.setPaymentStatus(com.erp.order.model.PaymentStatus.UNPAID);
        req.setItems(List.of(OrderItemRequest.builder().productId(testProduct.getId()).quantity(1).build()));
        OrderDto createdOrder = orderService.createOrder(req, "admin");

        // 3. Webhook lần 1: Thanh toán thành công & gạch nợ
        String transId = "MB-TRANS-" + System.nanoTime();
        com.erp.order.dto.PaymentWebhookRequest webhookReq = com.erp.order.dto.PaymentWebhookRequest.builder()
                .transactionId(transId)
                .orderCode(createdOrder.getOrderCode())
                .amount(createdOrder.getTotalAmount())
                .gateway("MB_BANK")
                .content("DH " + createdOrder.getOrderCode())
                .build();

        var res1 = vietQrService.handlePaymentWebhook(webhookReq);
        assertEquals(true, res1.get("success"));
        assertEquals("PAID", res1.get("status"));

        // 4. Webhook lần 2 với cùng transactionId => Idempotent Protection chặn xử lý đúp
        var res2 = vietQrService.handlePaymentWebhook(webhookReq);
        assertEquals(true, res2.get("success"));
        assertEquals("ALREADY_PROCESSED", res2.get("status"), "Lần 2 phải nhận diện là ALREADY_PROCESSED chống trừ nợ trùng lặp");
    }
}
