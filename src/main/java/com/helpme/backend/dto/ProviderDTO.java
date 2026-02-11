package com.helpme.backend.dto;

import com.helpme.backend.entity.KycStatus;
import com.helpme.backend.entity.Provider;
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
public class ProviderDTO {
    private UUID id;
    private String orgName;
    private Double baseLocationLat;
    private Double baseLocationLng;
    private String[] serviceTypes;
    private BigDecimal ratingAvg;
    private KycStatus kycStatus;
    private Boolean isOnline;
    private LocalDateTime createdAt;

    // User info
    private String phone;
    private String avatarUrl;

    /**
     * Convert Entity -> DTO
     */
    public static ProviderDTO from(Provider provider) {
        if (provider == null)
            return null;

        return ProviderDTO.builder()
                .id(provider.getId())
                .orgName(provider.getOrgName())
                .baseLocationLat(provider.getBaseLocation() != null ? provider.getBaseLocation().getY() : null)
                .baseLocationLng(provider.getBaseLocation() != null ? provider.getBaseLocation().getX() : null)
                .serviceTypes(provider.getServiceTypes())
                .ratingAvg(provider.getRatingAvg())
                .kycStatus(provider.getKycStatus())
                .isOnline(provider.getIsOnline())
                .createdAt(provider.getCreatedAt())
                .build();
    }
}