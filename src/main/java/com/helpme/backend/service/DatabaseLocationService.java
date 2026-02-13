// service/DatabaseLocationService.java
package com.helpme.backend.service;

import com.helpme.backend.entity.Provider;
import com.helpme.backend.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service("databaseLocationService")
@Primary // ✅ Make this the default when Redis is disabled
@ConditionalOnProperty(prefix = "spring.data.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseLocationService implements LocationService {

    private final ProviderRepository providerRepository;
    private final GeometryFactory geometryFactory;

    @Override
    public void updateLocation(UUID providerId, double lat, double lng) {
        try {
            Provider provider = providerRepository.findById(providerId)
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
            provider.setLiveLocation(location);
            provider.setLiveLocationUpdatedAt(LocalDateTime.now());
            providerRepository.save(provider);

            log.debug("✅ Database: Updated live_location for provider {}: ({}, {})", providerId, lat, lng);
        } catch (Exception e) {
            log.error("❌ Database: Failed to update location for provider {}: {}", providerId, e.getMessage());
        }
    }

    @Override
    public List<UUID> findNearby(double lat, double lng, int radiusMeters) {
        try {
            Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
            List<Provider> providers = providerRepository.findNearby(location, radiusMeters);

            List<UUID> providerIds = providers.stream()
                    .map(Provider::getId)
                    .collect(Collectors.toList());

            log.debug("✅ Database: Found {} nearby providers within {}m", providerIds.size(), radiusMeters);
            return providerIds;
        } catch (Exception e) {
            log.error("❌ Database: Failed to find nearby providers: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Point getLocation(UUID providerId) {
        try {
            Provider provider = providerRepository.findById(providerId).orElse(null);
            if (provider == null) {
                return null;
            }

            // Prefer live_location if recent (within 5 min)
            if (provider.getLiveLocation() != null &&
                    provider.getLiveLocationUpdatedAt() != null &&
                    provider.getLiveLocationUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {

                log.debug("📍 Using live_location for provider {}", providerId);
                return provider.getLiveLocation();
            }

            // Fallback to base_location
            log.debug("📍 Using base_location for provider {} (live location outdated)", providerId);
            return provider.getBaseLocation();

        } catch (Exception e) {
            log.error("❌ Database: Failed to get location for provider {}: {}", providerId, e.getMessage());
            return null;
        }
    }

    @Override
    public void removeLocation(UUID providerId) {
        try {
            Provider provider = providerRepository.findById(providerId).orElse(null);
            if (provider != null) {
                provider.setLiveLocation(null);
                provider.setLiveLocationUpdatedAt(null);
                providerRepository.save(provider);
                log.debug("✅ Database: Cleared live_location for provider {}", providerId);
            }
        } catch (Exception e) {
            log.error("❌ Database: Failed to remove location for provider {}: {}", providerId, e.getMessage());
        }
    }

    @Override
    public boolean hasLiveLocation(UUID providerId) {
        Point location = getLocation(providerId);
        return location != null;
    }
}