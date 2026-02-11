package com.helpme.backend.dto;

import com.helpme.backend.entity.AddOn;
import com.helpme.backend.entity.AddOnStatus;
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
public class AddOnDTO {
    private UUID id;
    private UUID orderId;
    private String reasonCode;
    private String reasonText;
    private BigDecimal amount;
    private AddOnStatus status;
    private String evidenceImage;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;

    /**
     * Convert Entity -> DTO
     */
    public static AddOnDTO from(AddOn addOn) {
        if (addOn == null)
            return null;

        return AddOnDTO.builder()
                .id(addOn.getId())
                .orderId(addOn.getOrderId())
                .reasonCode(addOn.getReasonCode())
                .reasonText(addOn.getReasonText())
                .amount(addOn.getAmount())
                .status(addOn.getStatus())
                .evidenceImage(addOn.getEvidenceImage())
                .createdAt(addOn.getCreatedAt())
                .approvedAt(addOn.getApprovedAt())
                .rejectedAt(addOn.getRejectedAt())
                .build();
    }
}