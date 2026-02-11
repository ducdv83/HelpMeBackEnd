package com.helpme.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCATION_KEY = "providers:location";
    private static final int LOCATION_TTL_MINUTES = 5;

    /**
     * Cập nhật vị trí LIVE của provider
     */
    public void updateLocation(UUID providerId, double lat, double lng) {
        try {
            Point point = new Point(lng, lat);

            redisTemplate.opsForGeo().add(
                    LOCATION_KEY,
                    point,
                    providerId.toString());

            // Set TTL cho key (tự động xóa sau 5 phút nếu không update)
            redisTemplate.expire(LOCATION_KEY, LOCATION_TTL_MINUTES, TimeUnit.MINUTES);

            log.debug("📍 Updated location for provider {}: ({}, {})", providerId, lat, lng);
        } catch (Exception e) {
            log.error("❌ Failed to update location for provider {}", providerId, e);
        }
    }

    /**
     * Tìm providers trong bán kính (meters)
     */
    public List<UUID> findNearby(double lat, double lng, int radiusMeters) {
        try {
            Point center = new Point(lng, lat);
            Distance radius = new Distance(radiusMeters, DistanceUnit.METERS);
            Circle area = new Circle(center, radius);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(
                    LOCATION_KEY,
                    area,
                    RedisGeoCommands.GeoRadiusCommandArgs
                            .newGeoRadiusArgs()
                            .sortAscending()
                            .includeDistance());

            if (results == null) {
                return List.of();
            }

            List<UUID> providerIds = results.getContent().stream()
                    .map(result -> {
                        String memberName = (String) result.getContent().getName();
                        return UUID.fromString(memberName);
                    })
                    .collect(Collectors.toList());

            log.debug("📍 Found {} providers near ({}, {}) within {}m",
                    providerIds.size(), lat, lng, radiusMeters);

            return providerIds;
        } catch (Exception e) {
            log.error("❌ Failed to find nearby providers", e);
            return List.of();
        }
    }

    /**
     * Xóa vị trí của provider (khi offline)
     */
    public void removeLocation(UUID providerId) {
        try {
            redisTemplate.opsForGeo().remove(LOCATION_KEY, providerId.toString());
            log.debug("📍 Removed location for provider {}", providerId);
        } catch (Exception e) {
            log.error("❌ Failed to remove location for provider {}", providerId, e);
        }
    }

    /**
     * Lấy vị trí hiện tại của provider
     */
    public Point getLocation(UUID providerId) {
        try {
            List<Point> positions = redisTemplate.opsForGeo().position(
                    LOCATION_KEY,
                    providerId.toString());

            if (positions != null && !positions.isEmpty()) {
                return positions.get(0);
            }
        } catch (Exception e) {
            log.error("❌ Failed to get location for provider {}", providerId, e);
        }

        return null;
    }

    /**
     * Kiểm tra provider có vị trí LIVE không
     */
    public boolean hasLiveLocation(UUID providerId) {
        return getLocation(providerId) != null;
    }
}