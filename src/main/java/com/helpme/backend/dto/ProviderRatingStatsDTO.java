package com.helpme.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRatingStatsDTO {
    private BigDecimal averageRating;
    private Long totalRatings;

    // Breakdown by stars: { "5": 10, "4": 5, "3": 2, "2": 1, "1": 0 }
    private Map<Integer, Long> ratingBreakdown;
}