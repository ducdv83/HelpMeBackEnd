package com.helpme.backend.dto;

import com.helpme.backend.entity.Rating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDTO {
    private UUID id;
    private UUID orderId;
    private UUID providerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Driver info (optional)
    private String driverName;
    private String driverAvatar;

    /**
     * Convert Entity -> DTO
     */
    public static RatingDTO from(Rating rating) {
        if (rating == null)
            return null;

        return RatingDTO.builder()
                .id(rating.getId())
                .orderId(rating.getOrderId())
                .providerId(rating.getProviderId())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}