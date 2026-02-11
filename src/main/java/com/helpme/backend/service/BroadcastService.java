package com.helpme.backend.service;

import com.helpme.backend.dto.OrderDTO;
import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.Provider;
import com.helpme.backend.repository.ProviderRepository;
import com.helpme.backend.websocket.SocketIOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final ProviderRepository providerRepository;
    private final RedisLocationService redisLocationService;
    private final SocketIOService socketService;
    private final NotificationService notificationService;

    /**
     * Broadcast order tới providers gần đó
     */
    public void notifyNearbyProviders(Order order) {
        Point location = order.getPickupLocation();
        int radius = order.getBroadcastRadius();

        double lat = location.getY();
        double lng = location.getX();

        // Step 1: Try Redis first (providers with LIVE location)
        List<UUID> liveProviderIds = redisLocationService.findNearby(lat, lng, radius);

        log.info("📍 Found {} providers with LIVE location near order {}",
                liveProviderIds.size(), order.getId());

        // Step 2: Fallback to PostGIS (providers with BASE location)
        List<Provider> baseProviders = providerRepository.findOnlineWithinRadius(lat, lng, radius);
        List<UUID> baseProviderIds = baseProviders.stream()
                .map(Provider::getId)
                .collect(Collectors.toList());

        log.info("📍 Found {} providers with BASE location near order {}",
                baseProviderIds.size(), order.getId());

        // Merge and deduplicate
        List<UUID> allProviderIds = liveProviderIds.stream()
                .collect(Collectors.toList());

        baseProviderIds.forEach(id -> {
            if (!allProviderIds.contains(id)) {
                allProviderIds.add(id);
            }
        });

        log.info("📢 Broadcasting order {} to {} providers",
                order.getId(), allProviderIds.size());

        // Step 3: Send notifications
        OrderDTO orderDTO = OrderDTO.fromBasic(order);

        allProviderIds.forEach(providerId -> {
            // Socket.IO realtime notification
            socketService.emitToUser(providerId, "new_order_broadcast", orderDTO);

            // Push notification as backup (if provider not connected to socket)
            if (!socketService.isUserOnline(providerId)) {
                sendPushNotificationToProvider(providerId);
            }
        });
    }

    /**
     * Helper: Gửi push notification cho provider
     */
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