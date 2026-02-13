package com.helpme.backend.service;

import com.helpme.backend.dto.OrderDTO;
import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.Provider;
import com.helpme.backend.repository.ProviderRepository;
import com.helpme.backend.websocket.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
// @RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final ProviderRepository providerRepository;
    private final SocketIOService socketIOService;
    private final LocationService locationService;  // ✅ Use interface, not RedisLocationService
    private final NotificationService notificationService;
    private final boolean redisEnabled;

    // ✅ Constructor with optional LocationService
    public BroadcastService(
        SocketIOService socketIOService,
        @Autowired(required = false) LocationService locationService,
        ProviderRepository providerRepository,
        @Value("${spring.data.redis.enabled:false}") boolean redisEnabled,
        NotificationService notificationService
    ) {
        this.socketIOService = socketIOService;
        this.locationService = locationService;
        this.providerRepository = providerRepository;
        this.redisEnabled = redisEnabled;
        this.notificationService = notificationService;
        
        log.info("🔧 BroadcastService initialized with Redis: {}", redisEnabled ? "ENABLED" : "DISABLED");
    }
    /**
     * Notify nearby providers about new order
     */
    public void notifyNearbyProviders(Order order) {
        try {
            List<UUID> nearbyProviderIds;
            Point location = order.getPickupLocation();
            int radius = order.getBroadcastRadius();

            double lat = location.getY();
            double lng = location.getX();

            if (redisEnabled && locationService != null) {
                // ✅ Use Redis for fast lookup
                nearbyProviderIds = locationService.findNearby(
                        lat,
                        lng,
                        radius
                );
                log.info("📍 Found {} nearby providers from Redis", nearbyProviderIds.size());
            } else {
                // ✅ Fallback to database (slower but works)
                nearbyProviderIds = findNearbyProvidersFromDatabase(order);
                log.info("📍 Found {} nearby providers from Database", nearbyProviderIds.size());
            }

            // Notify each nearby provider
            for (UUID providerId : nearbyProviderIds) {
                try {
                    socketIOService.emitToUser(
                            providerId,
                            "new_order_nearby",
                            order);
                    log.debug("✅ Notified provider {} about order {}", providerId, order.getId());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to notify provider {}: {}", providerId, e.getMessage());
                }
            }

            log.info("✅ Broadcast complete: Notified {} providers about order {}",
                    nearbyProviderIds.size(), order.getId());

        } catch (Exception e) {
            log.error("❌ Failed to broadcast order {}: {}", order.getId(), e.getMessage());
        }
    }

    /**
     * Fallback: Find nearby providers from database
     */
    private List<UUID> findNearbyProvidersFromDatabase(Order order) {
        List<Provider> providers = providerRepository.findNearby(
                order.getPickupLocation(),
                10000 // 10km radius
        );

        return providers.stream()
                .map(Provider::getId)
                .toList();
    }

    /**
     * Helper: Gửi push notification cho provider
     */
    @Deprecated
    private void sendPushNotificationToProvider(UUID providerId) {
        providerRepository.findById(providerId).ifPresent(provider -> {
            // Get push token from user
            String pushToken = null; // TODO: Get from User entity

            if (pushToken != null) {
                notificationService.sendPushNotification(
                        pushToken,
                        "Yêu cầu cứu hộ mới",
                        "Có yêu cầu cứu hộ gần bạn. Nhấn để xem chi tiết.");
            }
        });
    }
}