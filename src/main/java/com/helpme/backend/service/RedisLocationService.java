// service/RedisLocationService.java
package com.helpme.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service("redisLocationService")
@Primary // ✅ Override DatabaseLocationService when Redis enabled
@ConditionalOnProperty(prefix = "spring.data.redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisLocationService implements LocationService {

    private static final String LOCATION_KEY = "provider:location";
    private final RedisTemplate<String, Object> redisTemplate;
    private final GeometryFactory geometryFactory;

    @Override
    public void updateLocation(UUID providerId, double lat, double lng) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            geoOps.add(LOCATION_KEY, new org.springframework.data.geo.Point(lng, lat), providerId.toString());
            log.debug("✅ Redis: Updated location for provider {}: ({}, {})", providerId, lat, lng);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to update location for provider {}: {}", providerId, e.getMessage());
        }
    }

    @Override
    public List<UUID> findNearby(double lat, double lng, int radiusMeters) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();

            Distance radius = new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS);
            Circle area = new Circle(new org.springframework.data.geo.Point(lng, lat), radius);

            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .sortAscending()
                    .limit(50);

            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = geoOps.radius(LOCATION_KEY, area, args);

            if (results == null) {
                return List.of();
            }

            List<UUID> providerIds = results.getContent().stream()
                    .map(result -> UUID.fromString(result.getContent().getName().toString()))
                    .collect(Collectors.toList());

            log.debug("✅ Redis: Found {} nearby providers within {}m", providerIds.size(), radiusMeters);
            return providerIds;
        } catch (Exception e) {
            log.error("❌ Redis: Failed to find nearby providers: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Point getLocation(UUID providerId) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            List<org.springframework.data.geo.Point> positions = geoOps.position(LOCATION_KEY, providerId.toString());

            if (positions == null || positions.isEmpty() || positions.get(0) == null) {
                return null;
            }

            org.springframework.data.geo.Point point = positions.get(0);
            return geometryFactory.createPoint(new Coordinate(point.getX(), point.getY()));
        } catch (Exception e) {
            log.error("❌ Redis: Failed to get location for provider {}: {}", providerId, e.getMessage());
            return null;
        }
    }

    @Override
    public void removeLocation(UUID providerId) {
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            geoOps.remove(LOCATION_KEY, providerId.toString());
            log.debug("✅ Redis: Removed location for provider {}", providerId);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to remove location for provider {}: {}", providerId, e.getMessage());
        }
    }

    @Override
    public boolean hasLiveLocation(UUID providerId) {
        Point location = getLocation(providerId);
        return location != null;
    }
}