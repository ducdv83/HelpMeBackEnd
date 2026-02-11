package com.helpme.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApproveAddOnRequest {

    @NotNull(message = "AddOn ID is required")
    private UUID addOnId;
}