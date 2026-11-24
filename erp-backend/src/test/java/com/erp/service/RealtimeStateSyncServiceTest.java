package com.erp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealtimeStateSyncServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealtimeStateSyncService realtimeStateSyncService;

    @Test
    @DisplayName("Test phát sự kiện đơn hàng thành công qua STOMP /topic/orders")
    @SuppressWarnings("unchecked")
    void testPublishOrderEvent() {
        ArgumentCaptor<RealtimeStateSyncService.RealtimeEvent<Object>> captor =
                ArgumentCaptor.forClass(RealtimeStateSyncService.RealtimeEvent.class);

        Map<String, Object> orderData = Map.of(
                "orderCode", "ORD-20261124-001",
                "customerName", "Nguyen Van A",
                "totalAmount", 500000
        );

        realtimeStateSyncService.publishOrderEvent("CREATED", orderData);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/orders"), captor.capture());

        RealtimeStateSyncService.RealtimeEvent<Object> event = captor.getValue();
        assertEquals("/topic/orders", event.getTopic());
        assertEquals("CREATED", event.getAction());
        assertEquals(orderData, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Test phát sự kiện kho qua STOMP /topic/inventory")
    @SuppressWarnings("unchecked")
    void testPublishInventoryEvent() {
        ArgumentCaptor<RealtimeStateSyncService.RealtimeEvent<Object>> captor =
                ArgumentCaptor.forClass(RealtimeStateSyncService.RealtimeEvent.class);

        Map<String, Object> inventoryData = Map.of(
                "sku", "SKU-IPHONE15",
                "diffQty", 10
        );

        realtimeStateSyncService.publishInventoryEvent("STOCK_ADJUSTED", inventoryData);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/inventory"), captor.capture());

        RealtimeStateSyncService.RealtimeEvent<Object> event = captor.getValue();
        assertEquals("/topic/inventory", event.getTopic());
        assertEquals("STOCK_ADJUSTED", event.getAction());
        assertEquals(inventoryData, event.getData());
    }

    @Test
    @DisplayName("Test phát sự kiện quỹ tài chính qua STOMP /topic/finance")
    @SuppressWarnings("unchecked")
    void testPublishFinanceEvent() {
        ArgumentCaptor<RealtimeStateSyncService.RealtimeEvent<Object>> captor =
                ArgumentCaptor.forClass(RealtimeStateSyncService.RealtimeEvent.class);

        Map<String, Object> financeData = Map.of(
                "voucherCode", "TXN-9988",
                "amount", 2000000
        );

        realtimeStateSyncService.publishFinanceEvent("TRANSFER_COMPLETED", financeData);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/finance"), captor.capture());

        RealtimeStateSyncService.RealtimeEvent<Object> event = captor.getValue();
        assertEquals("/topic/finance", event.getTopic());
        assertEquals("TRANSFER_COMPLETED", event.getAction());
        assertEquals(financeData, event.getData());
    }
}
