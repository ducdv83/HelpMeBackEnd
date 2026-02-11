package com.helpme.backend.dto;

import com.helpme.backend.entity.LocationSource;
import com.helpme.backend.entity.Quote;
import com.helpme.backend.entity.QuoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDTO {
    private UUID id;
    private UUID orderId;
    private UUID providerId;
    private QuoteStatus status;
    private BigDecimal basePrice;
    private BigDecimal distancePrice;
    private BigDecimal materialPrice;
    private BigDecimal totalEst;
    private LocationSource locationSource;
    private Integer etaMinutes;
    private String notes;
    private LocalDateTime createdAt;

    // Provider info (optional)
    private String providerOrgName;
    private BigDecimal providerRating;
    private String providerPhone;
    private String providerAvatar;

    /**
     * Convert Entity -> DTO (basic)
     */
    public static QuoteDTO fromBasic(Quote quote) {
        if (quote == null)
            return null;

        return QuoteDTO.builder()
                .id(quote.getId())
                .orderId(quote.getOrderId())
                .providerId(quote.getProviderId())
                .status(quote.getStatus())
                .basePrice(quote.getBasePrice())
                .distancePrice(quote.getDistancePrice())
                .materialPrice(quote.getMaterialPrice())
                .totalEst(quote.getTotalEst())
                .locationSource(quote.getLocationSource())
                .etaMinutes(quote.getEtaMinutes())
                .notes(quote.getNotes())
                .createdAt(quote.getCreatedAt())
                .build();
    }
}