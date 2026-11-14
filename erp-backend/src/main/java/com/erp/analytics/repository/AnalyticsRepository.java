package com.erp.analytics.repository;

import com.erp.analytics.projection.InventoryValuationProjection;
import com.erp.analytics.projection.PaymentMethodProjection;
import com.erp.analytics.projection.RevenueTrendProjection;
import com.erp.analytics.projection.StatusCountProjection;
import com.erp.analytics.projection.TopProductProjection;
import com.erp.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Order, Long> {

    @Query(value = "SELECT " +
            " DATE_FORMAT(o.created_at, '%Y-%m-%d') AS orderDate, " +
            " COUNT(o.id) AS orderCount, " +
            " COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.total_amount ELSE 0 END), 0) AS revenue " +
            "FROM erp_orders o " +
            "WHERE o.created_at >= :fromDate AND o.created_at <= :toDate " +
            "GROUP BY DATE_FORMAT(o.created_at, '%Y-%m-%d') " +
            "ORDER BY orderDate ASC", nativeQuery = true)
    List<RevenueTrendProjection> getDailyRevenueTrends(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = "SELECT " +
            " i.product_id AS productId, " +
            " i.product_name AS productName, " +
            " i.product_sku AS productSku, " +
            " i.product_image_url AS productImageUrl, " +
            " SUM(i.quantity) AS totalQuantity, " +
            " SUM(i.line_total) AS totalRevenue, " +
            " COALESCE(p.stock, 0) AS currentStock " +
            "FROM erp_order_items i " +
            "JOIN erp_orders o ON i.order_id = o.id " +
            "LEFT JOIN products p ON i.product_id = p.id " +
            "WHERE o.status != 'CANCELLED' " +
            "  AND o.created_at >= :fromDate AND o.created_at <= :toDate " +
            "GROUP BY i.product_id, i.product_name, i.product_sku, i.product_image_url, p.stock " +
            "ORDER BY totalRevenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("limit") int limit
    );

    @Query(value = "SELECT " +
            " o.status AS status, " +
            " COUNT(o.id) AS count " +
            "FROM erp_orders o " +
            "WHERE o.created_at >= :fromDate AND o.created_at <= :toDate " +
            "GROUP BY o.status", nativeQuery = true)
    List<StatusCountProjection> getOrderStatusBreakdown(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = "SELECT " +
            " COALESCE(o.payment_method, 'CASH') AS paymentMethod, " +
            " COUNT(o.id) AS count, " +
            " COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.total_amount ELSE 0 END), 0) AS totalAmount " +
            "FROM erp_orders o " +
            "WHERE o.created_at >= :fromDate AND o.created_at <= :toDate " +
            "GROUP BY o.payment_method", nativeQuery = true)
    List<PaymentMethodProjection> getPaymentMethodBreakdown(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = "SELECT " +
            " COALESCE(SUM(p.price * p.stock), 0) AS totalValuation, " +
            " COALESCE(SUM(p.stock), 0) AS totalUnits, " +
            " COALESCE(SUM(CASE WHEN p.stock <= 10 THEN 1 ELSE 0 END), 0) AS lowStockCount " +
            "FROM products p", nativeQuery = true)
    InventoryValuationProjection getInventoryValuation();
}
