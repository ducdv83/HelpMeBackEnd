// service/ProviderService.java
package com.helpme.backend.service;

import com.helpme.backend.dto.OrderDTO;
import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.OrderStatus;
import com.helpme.backend.entity.Provider;
import com.helpme.backend.entity.User;
import com.helpme.backend.exception.ForbiddenException;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.OrderRepository;
import com.helpme.backend.repository.ProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final OrderRepository orderRepository;
    private final LocationService locationService; // ✅ Use interface
    private final GeometryFactory geometryFactory;
    private final boolean redisEnabled;

    // ✅ Constructor with optional LocationService
    public ProviderService(
            ProviderRepository providerRepository,
            OrderRepository orderRepository,
            @Autowired(required = false) LocationService locationService,
            GeometryFactory geometryFactory,
            @Value("${spring.data.redis.enabled:false}") boolean redisEnabled) {
        this.providerRepository = providerRepository;
        this.orderRepository = orderRepository;
        this.locationService = locationService;
        this.geometryFactory = geometryFactory;
        this.redisEnabled = redisEnabled;

        log.info("🔧 ProviderService initialized with Redis: {}", redisEnabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Update provider online status
     */
    @Transactional
    public void updateOnlineStatus(User user, boolean isOnline) {
        Provider provider = providerRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        provider.setIsOnline(isOnline);
        providerRepository.save(provider);

        // If going offline, remove from Redis
        if (!isOnline && redisEnabled && locationService != null) {
            locationService.removeLocation(provider.getId());
            log.info("📍 Removed provider {} from Redis (offline)", provider.getId());
        }

        log.info("✅ Provider {} status: {}", provider.getId(), isOnline ? "ONLINE" : "OFFLINE");
    }

    /**
     * Find nearby orders for provider
     */
    public List<OrderDTO> findNearbyOrders(double lat, double lng, int radius, User user) {
        Provider provider = providerRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        if (!provider.getIsOnline()) {
            log.warn("⚠️ Provider {} is offline, returning empty list", provider.getId());
            return List.of();
        }

        // Find nearby BROADCASTING orders from database
        // Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
        List<Order> nearbyOrders = orderRepository.findNearbyBroadcastingOrders(lat, lng, radius);

        log.info("📍 Found {} nearby orders for provider {} within {}m",
                nearbyOrders.size(), provider.getId(), radius);

        return nearbyOrders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update provider live location
     */
    @Transactional
    public void updateLiveLocation(User user, double lat, double lng) {
        Provider provider = providerRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        if (redisEnabled && locationService != null) {
            // ✅ Update in Redis (fast)
            locationService.updateLocation(provider.getId(), lat, lng);
            log.debug("📍 Updated live location in Redis for provider {}", provider.getId());
        } else {
            // ✅ Fallback: Update in database
            Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
            provider.setLiveLocation(location);
            provider.setLiveLocationUpdatedAt(java.time.LocalDateTime.now());
            providerRepository.save(provider);
            log.debug("📍 Updated live location in Database for provider {}", provider.getId());
        }
    }

    /**
     * Update order status (Provider side)
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, User providerUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Verify this provider owns the order
        if (!order.getProviderId().equals(providerUser.getId())) {
            throw new ForbiddenException("You are not assigned to this order");
        }

        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("✅ Order {} status updated: {} → {}", orderId, order.getStatus(), newStatus);
    }

    // ==================== PRIVATE METHODS ====================

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Add validation logic
        // Example: MATCHED → EN_ROUTE → ARRIVED → IN_SERVICE → COMPLETED
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setProviderId(order.getProviderId());
        dto.setStatus(order.getStatus());
        dto.setServiceType(order.getServiceType());
        dto.setDescription(order.getDescription());
        dto.setPickupLat(order.getPickupLocation().getY());
        dto.setPickupLng(order.getPickupLocation().getX());
        dto.setMediaUrls(order.getMediaUrls());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}