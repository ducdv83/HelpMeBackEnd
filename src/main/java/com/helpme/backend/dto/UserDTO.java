package com.helpme.backend.dto;

import com.helpme.backend.entity.User;
import com.helpme.backend.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String phone;
    private String fullName;
    private UserRole role;
    private String avatarUrl;
    private LocalDateTime createdAt;

    /**
     * Convert Entity -> DTO
     */
    public static UserDTO from(User user) {
        if (user == null)
            return null;

        return UserDTO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}