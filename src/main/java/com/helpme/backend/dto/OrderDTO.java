package com.helpme.backend.dto;

import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private UUID id;
    private UUID driverId;
    private UUID providerId;
    private OrderStatus status;
    private String serviceType;
    private Double pickupLat;
    private Double pickupLng;
    private String description;
    private List<String> mediaUrls;
    private BigDecimal finalAmount;
    private Integer broadcastRadius;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Driver info (optional)
    private String driverName;
    private String driverPhone;
    private String driverAvatar;

    // Provider info (optional)
    private String providerOrgName;
    private BigDecimal providerRating;
    private String providerPhone;
    private String providerAvatar;

    /**
     * Convert Entity -> DTO (basic)
     */
    public static OrderDTO fromBasic(Order order) {
        if (order == null)
            return null;

        return OrderDTO.builder()
                .id(order.getId())
                .driverId(order.getDriverId())
                .providerId(order.getProviderId())
                .status(order.getStatus())
                .serviceType(order.getServiceType())
                .pickupLat(order.getPickupLocation() != null ? order.getPickupLocation().getY() : null)
                .pickupLng(order.getPickupLocation() != null ? order.getPickupLocation().getX() : null)
                .description(order.getDescription())
                .mediaUrls(order.getMediaUrls())
                .finalAmount(order.getFinalAmount())
                .broadcastRadius(order.getBroadcastRadius())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}