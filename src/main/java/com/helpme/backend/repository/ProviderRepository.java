package com.helpme.backend.repository;

import com.helpme.backend.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
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

    /**
     * Find nearby providers using HYBRID location strategy:
     * - Use live_location if updated within threshold (Redis fallback)
     * - Otherwise use base_location
     */
    @Query(value = """
            SELECT p.*
            FROM providers p
            WHERE p.is_online = true
              AND (
                -- Use live_location if recent
                (p.live_location IS NOT NULL
                 AND p.live_location_updated_at > :liveLocationThreshold
                 AND ST_DWithin(
                   p.live_location::geography,
                   ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography,
                   :radiusMeters
                 )
                )
                OR
                -- Fallback to base_location
                (p.base_location IS NOT NULL
                 AND (p.live_location IS NULL OR p.live_location_updated_at <= :liveLocationThreshold)
                 AND ST_DWithin(
                   p.base_location::geography,
                   ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography,
                   :radiusMeters
                 )
                )
              )
            ORDER BY
              CASE
                WHEN p.live_location IS NOT NULL AND p.live_location_updated_at > :liveLocationThreshold
                THEN ST_Distance(
                  p.live_location::geography,
                  ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography
                )
                ELSE ST_Distance(
                  p.base_location::geography,
                  ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography
                )
              END ASC
            LIMIT 50
            """, nativeQuery = true)
    List<Provider> findNearbyWithLiveLocation(
            @Param("location") Point location,
            @Param("radiusMeters") int radiusMeters,
            @Param("liveLocationThreshold") LocalDateTime liveLocationThreshold);

    /**
     * Find nearby online providers using base_location or live_location
     * Uses PostGIS ST_DWithin for geographic search
     * 
     * @param location     Center point to search from
     * @param radiusMeters Radius in meters
     * @return List of providers within radius, sorted by distance
     */
    @Query(value = """
        SELECT p.*
        FROM providers p
        WHERE p.is_online = true
          AND (
            -- Use live_location if it exists and is recent (within 5 minutes)
            (p.live_location IS NOT NULL
             AND p.live_location_updated_at > NOW() - INTERVAL '5 minutes'
             AND ST_DWithin(
               p.live_location::geography,
               ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography,
               :radiusMeters
             )
            )
            OR
            -- Fallback to base_location
            (p.base_location IS NOT NULL
             AND (p.live_location IS NULL OR p.live_location_updated_at <= NOW() - INTERVAL '5 minutes')
             AND ST_DWithin(
               p.base_location::geography,
               ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography,
               :radiusMeters
             )
            )
          )
        ORDER BY
          CASE
            WHEN p.live_location IS NOT NULL AND p.live_location_updated_at > NOW() - INTERVAL '5 minutes'
            THEN ST_Distance(
              p.live_location::geography,
              ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography
            )
            ELSE ST_Distance(
              p.base_location::geography,
              ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography
            )
          END ASC
        LIMIT 50
        """, nativeQuery = true)
    List<Provider> findNearby(
        @Param("location") Point location,
        @Param("radiusMeters") int radiusMeters);

    /**
     * Simpler version: Find nearby providers using only base_location
     * Use this if live_location is not implemented yet
     */
    @Query(value = """
        SELECT p.*
        FROM providers p
        WHERE p.is_online = true
          AND p.base_location IS NOT NULL
          AND ST_DWithin(
            p.base_location::geography,
            ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography,
            :radiusMeters
          )
        ORDER BY ST_Distance(
            p.base_location::geography,
            ST_SetSRID(ST_MakePoint(:#{#location.x}, :#{#location.y}), 4326)::geography
        ) ASC
        LIMIT 50
        """, nativeQuery = true)
    List<Provider> findNearbyByBaseLocation(
        @Param("location") Point location,
        @Param("radiusMeters") int radiusMeters);
}