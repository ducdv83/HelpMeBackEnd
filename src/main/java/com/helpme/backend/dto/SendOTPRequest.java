package com.helpme.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOTPRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Phone number must be 10 digits starting with 0")
    private String phone;
}