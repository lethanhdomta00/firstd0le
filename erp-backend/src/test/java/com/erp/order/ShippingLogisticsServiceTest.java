package com.erp.order;

import com.erp.order.dto.DispatchCarrierRequest;
import com.erp.order.dto.OrderDto;
import com.erp.order.dto.ShippingTrackingDto;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.OrderService;
import com.erp.order.service.ShippingLogisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingLogisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ShippingLogisticsService shippingLogisticsService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setId(100L);
        sampleOrder.setOrderCode("ORD-202611-0001");
        sampleOrder.setCustomerName("Đại lý Minh Thắng");
        sampleOrder.setCustomerPhone("0988776655");
        sampleOrder.setShippingAddress("Số 15 Lê Văn Lương, Thanh Xuân, Hà Nội");
        sampleOrder.setStatus(OrderStatus.CONFIRMED);
        sampleOrder.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder.setTotalAmount(new BigDecimal("1500000"));
    }

    @Test
    @DisplayName("Bàn giao đơn vị vận chuyển GHN thành công, sinh mã tracking và cập nhật trạng thái SHIPPING")
    void testDispatchOrderToGhn() {
        DispatchCarrierRequest req = DispatchCarrierRequest.builder()
                .carrier("GHN")
                .shippingFee(new BigDecimal("35000"))
                .codAmount(BigDecimal.ZERO)
                .note("Giao giờ hành chính")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderService.toOrderDto(any(Order.class))).thenReturn(OrderDto.builder()
                .id(100L)
                .orderCode("ORD-202611-0001")
                .status(OrderStatus.SHIPPING)
                .shippingCarrier("GHN")
                .trackingCode("GHN-VN12345678")
                .build());

        OrderDto result = shippingLogisticsService.dispatchOrderToCarrier(100L, req, "admin");

        assertNotNull(result);
        assertEquals(OrderStatus.SHIPPING, sampleOrder.getStatus());
        assertEquals("GHN", sampleOrder.getShippingCarrier());
        assertNotNull(sampleOrder.getTrackingCode());
        assertTrue(sampleOrder.getTrackingCode().startsWith("GHN-VN"));
        assertEquals("IN_TRANSIT", sampleOrder.getShippingStatus());
        assertNotNull(sampleOrder.getDispatchedAt());

        verify(orderRepository).save(sampleOrder);
    }

    @Test
    @DisplayName("Chặn bàn giao đơn vị vận chuyển nếu đơn hàng đã bị hủy")
    void testCannotDispatchCancelledOrder() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        DispatchCarrierRequest req = DispatchCarrierRequest.builder().carrier("GHN").build();

        assertThrows(IllegalStateException.class, () ->
                shippingLogisticsService.dispatchOrderToCarrier(100L, req, "admin")
        );
    }

    @Test
    @DisplayName("Tra cứu tiến trình vận đơn trả về timeline các mốc vận chuyển")
    void testGetShippingTracking() {
        sampleOrder.setShippingCarrier("GHN");
        sampleOrder.setTrackingCode("GHN-VN88991122");
        sampleOrder.setShippingStatus("IN_TRANSIT");

        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        ShippingTrackingDto tracking = shippingLogisticsService.getShippingTracking(100L);

        assertNotNull(tracking);
        assertEquals("ORD-202611-0001", tracking.getOrderCode());
        assertEquals("GHN-VN88991122", tracking.getTrackingCode());
        assertFalse(tracking.getEvents().isEmpty());
    }
}
