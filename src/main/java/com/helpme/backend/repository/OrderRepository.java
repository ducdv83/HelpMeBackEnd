package com.helpme.backend.repository;

import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Lấy danh sách orders của driver (sắp xếp mới nhất trước)
     */
    List<Order> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    /**
     * Lấy danh sách orders của provider
     */
    List<Order> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    /**
     * Lấy order theo ID và kiểm tra ownership của driver
     */
    Optional<Order> findByIdAndDriverId(UUID orderId, UUID driverId);

    /**
     * Lấy order theo ID và kiểm tra ownership của provider
     */
    Optional<Order> findByIdAndProviderId(UUID orderId, UUID providerId);

    /**
     * Tìm các orders "cũ" (stale) - đang ở trạng thái BROADCASTING quá lâu
     * Dùng cho scheduled task auto-notify hoặc auto-cancel
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status " +
            "AND o.createdAt < :cutoffTime")
    List<Order> findStaleOrders(
            @Param("status") OrderStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Tìm các orders đang BROADCASTING trong bán kính
     * Dùng cho Provider tìm kiếm orders gần họ
     */
    @Query(value = """
            SELECT o.* FROM orders o
            WHERE o.status = 'BROADCASTING'
            AND ST_DWithin(
                o.pickup_location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
            ORDER BY o.created_at DESC
            """, nativeQuery = true)
    List<Order> findNearbyBroadcastingOrders(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters);

    /**
     * Đếm số orders theo status của driver
     */
    long countByDriverIdAndStatus(UUID driverId, OrderStatus status);

    /**
     * Lấy order đang active của driver (chỉ có 1 order active tại 1 thời điểm)
     */
    @Query("SELECT o FROM Order o WHERE o.driverId = :driverId " +
            "AND o.status IN ('BROADCASTING', 'MATCHED', 'EN_ROUTE', 'ARRIVED', 'IN_SERVICE') " +
            "ORDER BY o.createdAt DESC")
    Optional<Order> findActiveOrderByDriverId(@Param("driverId") UUID driverId);

    /**
     * Lấy order đang active của provider
     */
    @Query("SELECT o FROM Order o WHERE o.providerId = :providerId " +
            "AND o.status IN ('MATCHED', 'EN_ROUTE', 'ARRIVED', 'IN_SERVICE') " +
            "ORDER BY o.createdAt DESC")
    Optional<Order> findActiveOrderByProviderId(@Param("providerId") UUID providerId);
}