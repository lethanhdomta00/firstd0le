package com.erp.order.repository;

import com.erp.order.dto.OrderStatsAggregate;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCode(String orderCode);

    @Query("SELECT new com.erp.order.dto.OrderStatsAggregate(" +
           " COUNT(o.id), " +
           " SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.PENDING THEN 1L ELSE 0L END), " +
           " SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.CONFIRMED THEN 1L ELSE 0L END), " +
           " SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.SHIPPING THEN 1L ELSE 0L END), " +
           " SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.COMPLETED THEN 1L ELSE 0L END), " +
           " SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.CANCELLED THEN 1L ELSE 0L END), " +
           " SUM(CASE WHEN o.createdAt >= :startOfToday THEN 1L ELSE 0L END), " +
           " COALESCE(SUM(CASE WHEN o.status != com.erp.order.model.OrderStatus.CANCELLED THEN o.totalAmount ELSE 0 END), 0), " +
           " COALESCE(SUM(CASE WHEN o.status = com.erp.order.model.OrderStatus.COMPLETED THEN o.totalAmount ELSE 0 END), 0), " +
           " COALESCE(SUM(CASE WHEN o.paymentStatus = com.erp.order.model.PaymentStatus.PAID THEN o.totalAmount ELSE 0 END), 0) " +
           ") FROM Order o")
    OrderStatsAggregate getOrderStatsAggregate(@Param("startOfToday") LocalDateTime startOfToday);

    @Query("SELECT o FROM Order o WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(COALESCE(o.customerEmail, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:fromDate IS NULL OR o.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR o.createdAt <= :toDate)")
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllForExport(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );
}
