package com.helpme.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Service type is required")
    private String serviceType;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double lat;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double lng;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    // Media URLs sẽ được upload trước qua /v1/files/upload
    private List<String> mediaUrls;

    @Min(value = 1000, message = "Broadcast radius must be at least 1km (1000m)")
    @Max(value = 50000, message = "Broadcast radius must be at most 50km (50000m)")
    private Integer broadcastRadius = 10000; // Default 10km
}