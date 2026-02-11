package com.helpme.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class CreateAddOnRequest {

    @NotBlank(message = "Reason code is required")
    private String reasonCode;

    @Size(max = 500, message = "Reason text must not exceed 500 characters")
    private String reasonText;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    // Evidence image (optional)
    private MultipartFile evidenceImage;
}