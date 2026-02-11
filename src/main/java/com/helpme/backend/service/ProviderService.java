package com.helpme.backend.service;

import com.helpme.backend.dto.OrderDTO;
import com.helpme.backend.entity.*;
import com.helpme.backend.exception.BadRequestException;
import com.helpme.backend.exception.ForbiddenException;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.*;
import com.helpme.backend.websocket.SocketIOService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RedisLocationService redisLocationService;  // ✅ Add this
    private final SocketIOService socketService;              // ✅ Add this
    
    /**
     * Update online/offline status
     */
    public void updateOnlineStatus(User user, boolean isOnline) {
        Provider provider = providerRepository.findById(user.getId())
            .orElseThrow(() -> new NotFoundException("Provider not found"));
        
        providerRepository.updateOnlineStatus(provider.getId(), isOnline);
        
        // ✅ Remove from Redis if going offline
        if (!isOnline) {
            redisLocationService.removeLocation(provider.getId());
        }
        
        log.info("✅ Provider {} is now {}", provider.getId(), isOnline ? "ONLINE" : "OFFLINE");
    }

    /**
     * Tìm orders gần provider
     */
    public List<OrderDTO> findNearbyOrders(double lat, double lng, int radius, User user) {
        // Validate provider
        providerRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        List<Order> orders = orderRepository.findNearbyBroadcastingOrders(lat, lng, radius);

        return orders.stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update live location
     */
    public void updateLiveLocation(User user, double lat, double lng) {
        Provider provider = providerRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        if (!provider.getIsOnline()) {
            throw new BadRequestException("Provider is offline");
        }

        redisLocationService.updateLocation(provider.getId(), lat, lng);
        log.debug("📍 Updated live location for provider {}", provider.getId());
    }

    /**
     * Update order status (Provider only)
     */
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, User providerUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getProviderId() == null || !order.getProviderId().equals(providerUser.getId())) {
            throw new ForbiddenException("Not your order");
        }

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("✅ Order {} status updated to {} by provider {}",
                orderId, newStatus, providerUser.getId());

        // ✅ Notify driver via Socket.IO
        socketService.emitToUser(
                order.getDriverId(),
                "order_status_update",
                Map.of(
                        "orderId", order.getId(),
                        "status", newStatus.name(),
                        "message", getStatusMessage(newStatus)));
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case EN_ROUTE -> "Thợ cứu hộ đang trên đường đến!";
            case ARRIVED -> "Thợ cứu hộ đã đến vị trí của bạn";
            case IN_SERVICE -> "Đang tiến hành sửa chữa";
            case COMPLETED -> "Hoàn thành dịch vụ";
            case CANCELLED -> "Đơn hàng đã bị hủy";
            default -> "Trạng thái đơn hàng đã thay đổi";
        };
    }

    /**
     * Validate order status transition
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        Map<OrderStatus, List<OrderStatus>> validTransitions = Map.of(
                OrderStatus.MATCHED, List.of(OrderStatus.EN_ROUTE, OrderStatus.CANCELLED),
                OrderStatus.EN_ROUTE, List.of(OrderStatus.ARRIVED, OrderStatus.CANCELLED),
                OrderStatus.ARRIVED, List.of(OrderStatus.IN_SERVICE, OrderStatus.CANCELLED),
                OrderStatus.IN_SERVICE, List.of(OrderStatus.COMPLETED),
                OrderStatus.COMPLETED, List.of());

        List<OrderStatus> allowed = validTransitions.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new BadRequestException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    /**
     * Helper: Convert Order -> OrderDTO
     */
    private OrderDTO toOrderDTO(Order order) {
        OrderDTO dto = OrderDTO.fromBasic(order);

        // Fetch driver info
        if (order.getDriverId() != null) {
            userRepository.findById(order.getDriverId())
                    .ifPresent(user -> {
                        dto.setDriverName(user.getFullName());
                        dto.setDriverPhone(user.getPhone());
                        dto.setDriverAvatar(user.getAvatarUrl());
                    });
        }

        return dto;
    }
}