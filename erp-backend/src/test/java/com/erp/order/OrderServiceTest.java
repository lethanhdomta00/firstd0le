package com.erp.order;

import com.erp.model.Product;
import com.erp.order.dto.*;
import com.erp.order.model.*;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.OrderExportService;
import com.erp.order.service.OrderService;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Doc Reader Pro Wireless");
        sampleProduct.setSku("SKU-9178");
        sampleProduct.setPrice(new BigDecimal("100000"));
        sampleProduct.setStock(8);
    }

    @Test
    void createOrder_deductsStockCalculatesVatAndRecordsLog() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Công ty CP Giải pháp Doanh nghiệp An Phát");
        request.setCustomerPhone("0909 888 777");
        request.setCustomerEmail("muahang@anphat.vn");
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setPaymentStatus(PaymentStatus.PAID);
        request.setDiscountAmount(BigDecimal.ZERO);
        request.setTaxRate(new BigDecimal("8"));

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);
        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(1L);
        item2.setQuantity(1); // Duplicate productId should merge to 3
        request.setItems(List.of(item1, item2));

        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderDto result = orderService.createOrder(request, "admin");

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(5, sampleProduct.getStock()); // 8 - 3 = 5
        assertEquals(new BigDecimal("300000.00"), result.getSubtotal());
        assertEquals(new BigDecimal("24000.00"), result.getTaxAmount());
        assertEquals(new BigDecimal("324000.00"), result.getTotalAmount());
        assertEquals(3, result.getTotalQuantity());
        assertEquals(1, result.getItems().size());
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    void createOrder_throwsWhenInsufficientStock() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Khách A");
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(99);
        request.setItems(List.of(item));

        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request, "admin")
        );

        assertTrue(ex.getMessage().contains("chỉ còn 8"));
        assertEquals(8, sampleProduct.getStock());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_restoresStockAndRefundsPaidOrder() {
        Order existing = new Order();
        existing.setId(5L);
        existing.setOrderCode("ORD-20261112-1001");
        existing.setCustomerName("Khách B");
        existing.setStatus(OrderStatus.CONFIRMED);
        existing.setPaymentStatus(PaymentStatus.PAID);
        existing.setStockDeducted(true);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(11L);
        orderItem.setProductId(1L);
        orderItem.setProductName("Doc Reader Pro Wireless");
        orderItem.setProductSku("SKU-9178");
        orderItem.setUnitPrice(new BigDecimal("100000"));
        orderItem.setQuantity(3);
        orderItem.setLineTotal(new BigDecimal("300000"));
        existing.addItem(orderItem);

        sampleProduct.setStock(5);

        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto cancelled = orderService.updateOrderStatus(5L, OrderStatus.CANCELLED, "Khách hủy", "admin");

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(PaymentStatus.REFUNDED, cancelled.getPaymentStatus());
        assertFalse(cancelled.isStockDeducted());
        assertEquals(8, sampleProduct.getStock()); // 5 + 3 restored
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    void updateOrderStatus_blocksTransitionFromCompleted() {
        Order completed = new Order();
        completed.setId(9L);
        completed.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(completed));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.updateOrderStatus(9L, OrderStatus.CANCELLED, "test", "admin")
        );
    }

    @Test
    void adjustStock_preventsNegativeStock() {
        StockAdjustRequest req = new StockAdjustRequest();
        req.setProductId(1L);
        req.setQuantityDelta(-15);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleProduct));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.adjustStock(req, "admin")
        );
    }

    @Test
    void exportService_generatesMultiSheetExcelXmlAndUtf8Csv() {
        Order order = new Order();
        order.setOrderCode("ORD-20261112-9999");
        order.setCustomerName("Khách Hàng Test");
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        order.setSubtotal(new BigDecimal("100000"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(new BigDecimal("8000"));
        order.setTotalAmount(new BigDecimal("108000"));
        order.setCreatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProductName("Doc");
        item.setQuantity(1);
        order.addItem(item);

        when(orderRepository.findAllForExport(null, null, null)).thenReturn(List.of(order));
        when(inventoryLogRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());

        OrderExportService exportService = new OrderExportService(orderRepository, inventoryLogRepository);
        String xml = new String(exportService.exportOrdersExcelXml(null, null, null), StandardCharsets.UTF_8);
        String csv = new String(exportService.exportOrdersCsv(null, null, null), StandardCharsets.UTF_8);

        assertTrue(xml.contains("<Worksheet ss:Name=\"Don Hang ERP\">"));
        assertTrue(xml.contains("<Worksheet ss:Name=\"Lich Su Xuat Nhap Kho\">"));
        assertTrue(xml.contains("ORD-20261112-9999"));
        assertTrue(csv.contains("ORD-20261112-9999"));
    }
}
