package com.erp.analytics;

import com.erp.analytics.dto.AnalyticsOverviewDto;
import com.erp.analytics.projection.InventoryValuationProjection;
import com.erp.analytics.projection.RevenueTrendProjection;
import com.erp.analytics.projection.StatusCountProjection;
import com.erp.analytics.projection.TopProductProjection;
import com.erp.analytics.repository.AnalyticsRepository;
import com.erp.analytics.service.AnalyticsService;
import com.erp.order.dto.OrderStatsAggregate;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getOverview_aggregatesCorrectlyWithoutRamLoop() {
        OrderStatsAggregate statsAgg = new OrderStatsAggregate(
                20L, 5L, 3L, 2L, 8L, 2L, 4L,
                new BigDecimal("1500000"),
                new BigDecimal("800000"),
                new BigDecimal("800000")
        );
        when(orderRepository.getOrderStatsAggregate(any())).thenReturn(statsAgg);

        InventoryValuationProjection valProj = mock(InventoryValuationProjection.class);
        when(valProj.getTotalValuation()).thenReturn(new BigDecimal("50000000"));
        when(valProj.getTotalUnits()).thenReturn(120L);
        when(valProj.getLowStockCount()).thenReturn(3L);
        when(analyticsRepository.getInventoryValuation()).thenReturn(valProj);

        RevenueTrendProjection trendProj = mock(RevenueTrendProjection.class);
        when(trendProj.getOrderDate()).thenReturn(LocalDate.now().toString());
        when(trendProj.getOrderCount()).thenReturn(2L);
        when(trendProj.getRevenue()).thenReturn(new BigDecimal("200000"));
        when(analyticsRepository.getDailyRevenueTrends(any(), any())).thenReturn(List.of(trendProj));

        StatusCountProjection stProj = mock(StatusCountProjection.class);
        when(stProj.getStatus()).thenReturn("COMPLETED");
        when(stProj.getCount()).thenReturn(8L);
        when(analyticsRepository.getOrderStatusBreakdown(any(), any())).thenReturn(List.of(stProj));

        when(analyticsRepository.getPaymentMethodBreakdown(any(), any())).thenReturn(List.of());
        when(analyticsRepository.getTopSellingProducts(any(), any(), eq(8))).thenReturn(List.of());
        when(productRepository.findTopLowStockProducts(eq(10), any())).thenReturn(List.of());

        AnalyticsOverviewDto overview = analyticsService.getOverview(LocalDate.now().minusDays(6), LocalDate.now());

        assertNotNull(overview);
        assertEquals(20L, overview.getTotalOrders());
        assertEquals(8L, overview.getCompletedOrders());
        assertEquals(new BigDecimal("1500000"), overview.getTotalRevenue());
        assertEquals(new BigDecimal("50000000"), overview.getInventoryValuation());
        assertEquals(7, overview.getRevenueTrends().size()); // 7 days interval
        assertEquals(40.0, overview.getCompletionRate());

        verify(orderRepository, times(1)).getOrderStatsAggregate(any());
        verify(analyticsRepository, times(1)).getInventoryValuation();
        verify(analyticsRepository, times(1)).getDailyRevenueTrends(any(), any());
    }

    @Test
    void exportAnalyticsExcelXml_generatesValidMultiSheetXml() {
        OrderStatsAggregate statsAgg = new OrderStatsAggregate(
                5L, 1L, 1L, 0L, 3L, 0L, 1L,
                new BigDecimal("500000"),
                new BigDecimal("300000"),
                new BigDecimal("300000")
        );
        when(orderRepository.getOrderStatsAggregate(any())).thenReturn(statsAgg);

        TopProductProjection topProj = mock(TopProductProjection.class);
        when(topProj.getProductId()).thenReturn(1L);
        when(topProj.getProductName()).thenReturn("Doc Reader");
        when(topProj.getProductSku()).thenReturn("SKU-9178");
        when(topProj.getTotalQuantity()).thenReturn(5L);
        when(topProj.getTotalRevenue()).thenReturn(new BigDecimal("500000"));
        when(topProj.getCurrentStock()).thenReturn(10L);
        when(analyticsRepository.getTopSellingProducts(any(), any(), eq(8))).thenReturn(List.of(topProj));

        when(analyticsRepository.getDailyRevenueTrends(any(), any())).thenReturn(List.of());
        when(analyticsRepository.getOrderStatusBreakdown(any(), any())).thenReturn(List.of());
        when(analyticsRepository.getPaymentMethodBreakdown(any(), any())).thenReturn(List.of());
        when(productRepository.findTopLowStockProducts(eq(10), any())).thenReturn(List.of());

        byte[] xmlBytes = analyticsService.exportAnalyticsExcelXml(LocalDate.now().minusDays(7), LocalDate.now());
        String xml = new String(xmlBytes, StandardCharsets.UTF_8);

        assertTrue(xml.contains("<Worksheet ss:Name=\"Doanh Thu Theo Ngay\">"));
        assertTrue(xml.contains("<Worksheet ss:Name=\"Top San Pham Ban Chay\">"));
        assertTrue(xml.contains("<Worksheet ss:Name=\"Canh Bao Ton Kho Thap\">"));
        assertTrue(xml.contains("Doc Reader"));
    }
}
