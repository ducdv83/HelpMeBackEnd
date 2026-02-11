package com.helpme.backend.repository;

import com.helpme.backend.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    /**
     * Tìm các provider ONLINE trong bán kính radiusMeters
     * Sử dụng PostGIS ST_DWithin cho hiệu năng tốt
     */
    @Query(value = """
            SELECT p.* FROM providers p
            WHERE p.is_online = true
            AND ST_DWithin(
                p.base_location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
            ORDER BY ST_Distance(
                p.base_location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            ) ASC
            """, nativeQuery = true)
    List<Provider> findOnlineWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters);

    /**
     * Cập nhật trạng thái online/offline
     */
    @Modifying
    @Query("UPDATE Provider p SET p.isOnline = :status WHERE p.id = :id")
    void updateOnlineStatus(@Param("id") UUID id, @Param("status") boolean status);

    /**
     * Lấy danh sách providers đang online
     */
    List<Provider> findByIsOnlineTrue();
}