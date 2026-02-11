package com.helpme.backend.dto;

import com.helpme.backend.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPhoneResponse {
    private boolean exists;
    private UserRole role;
    private String fullName;
}