package com.helpme.backend.dto;

import com.helpme.backend.entity.LocationSource;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateQuoteRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", message = "Distance price must be non-negative")
    private BigDecimal distancePrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Material price must be non-negative")
    private BigDecimal materialPrice = BigDecimal.ZERO;

    @NotNull(message = "Location source is required")
    private LocationSource locationSource;

    @Min(value = 1, message = "ETA must be at least 1 minute")
    @Max(value = 600, message = "ETA must be at most 600 minutes (10 hours)")
    private Integer etaMinutes;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}