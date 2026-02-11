package com.helpme.backend.repository;

import com.helpme.backend.entity.AddOn;
import com.helpme.backend.entity.AddOnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, UUID> {

    /**
     * Lấy tất cả add-ons của một order
     */
    List<AddOn> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Lấy add-ons theo status
     */
    List<AddOn> findByOrderIdAndStatus(UUID orderId, AddOnStatus status);

    /**
     * Đếm số add-ons pending của một order
     */
    long countByOrderIdAndStatus(UUID orderId, AddOnStatus status);

    /**
     * Tính tổng số tiền add-ons đã APPROVED của một order
     */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AddOn a " +
            "WHERE a.orderId = :orderId AND a.status = 'APPROVED'")
    BigDecimal sumApprovedAmountByOrderId(@Param("orderId") UUID orderId);
}