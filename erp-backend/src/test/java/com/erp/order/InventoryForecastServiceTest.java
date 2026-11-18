package com.erp.order;

import com.erp.model.Product;
import com.erp.order.dto.InventoryForecastDto;
import com.erp.order.model.Order;
import com.erp.order.model.OrderItem;
import com.erp.order.model.OrderStatus;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.InventoryForecastService;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryForecastServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InventoryForecastService inventoryForecastService;

    private Product productCritical;
    private Product productSafe;

    @BeforeEach
    void setUp() {
        productCritical = new Product();
        productCritical.setId(1L);
        productCritical.setName("Áo Sơ Mi Nam");
        productCritical.setSku("SHIRT-01");
        productCritical.setBaseUnit("Cái");
        productCritical.setPhysicalStock(2);
        productCritical.setReservedStock(0);
        productCritical.setStock(2);
        productCritical.setMinStockAlert(10);
        productCritical.setMovingAvgCost(new BigDecimal("150000"));

        productSafe = new Product();
        productSafe.setId(2L);
        productSafe.setName("Dây Lưng Da Bò");
        productSafe.setSku("BELT-02");
        productSafe.setBaseUnit("Cái");
        productSafe.setPhysicalStock(200);
        productSafe.setReservedStock(0);
        productSafe.setStock(200);
        productSafe.setMinStockAlert(10);
        productSafe.setMovingAvgCost(new BigDecimal("80000"));
    }

    @Test
    @DisplayName("Dự báo cạn kho AI phân loại chính xác mặt hàng khẩn cấp CRITICAL và an toàn SAFE")
    void testInventoryForecastCalculation() {
        // Giả lập đơn hàng bán 60 cái Áo Sơ Mi trong 30 ngày qua (2 cái/ngày -> 2 cái tồn còn lại chỉ đủ 1 ngày -> CRITICAL)
        Order sampleOrder = new Order();
        sampleOrder.setId(10L);
        sampleOrder.setStatus(OrderStatus.COMPLETED);

        OrderItem itemShirt = new OrderItem();
        itemShirt.setProductId(1L);
        itemShirt.setQuantity(60);
        itemShirt.setUomConversionRate(BigDecimal.ONE);
        sampleOrder.addItem(itemShirt);

        when(productRepository.findAll()).thenReturn(List.of(productCritical, productSafe));
        when(orderRepository.findRecentOrders(any(LocalDateTime.class), eq(OrderStatus.CANCELLED)))
                .thenReturn(List.of(sampleOrder));

        List<InventoryForecastDto> forecast = inventoryForecastService.getInventoryForecast();

        assertNotNull(forecast);
        assertEquals(2, forecast.size());

        // Mặt hàng CRITICAL xếp đầu tiên
        InventoryForecastDto first = forecast.get(0);
        assertEquals(1L, first.getProductId());
        assertEquals("CRITICAL", first.getRiskLevel());
        assertEquals(60, first.getTotalSoldLast30Days());
        assertEquals(2.0, first.getAvgDailySales());
        assertEquals(1, first.getDaysOfStockRemaining());
        assertTrue(first.getSuggestedRestockQty() > 0);

        // Mặt hàng SAFE xếp sau
        InventoryForecastDto second = forecast.get(1);
        assertEquals(2L, second.getProductId());
        assertEquals("SAFE", second.getRiskLevel());
    }
}
