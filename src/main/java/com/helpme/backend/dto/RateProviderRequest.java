package com.helpme.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RateProviderRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}