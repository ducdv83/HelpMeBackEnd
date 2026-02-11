package com.helpme.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpme.backend.dto.AuthResponse;
import com.helpme.backend.dto.CheckPhoneResponse;
import com.helpme.backend.dto.UserDTO;
import com.helpme.backend.dto.VerifyOTPRequest;
import com.helpme.backend.entity.Provider;
import com.helpme.backend.entity.User;
import com.helpme.backend.entity.UserRole;
import com.helpme.backend.repository.ProviderRepository;
import com.helpme.backend.repository.UserRepository;
import com.helpme.backend.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final OTPService otpService;
    private final JwtUtil jwtUtil;

    /**
     * Kiểm tra số điện thoại đã tồn tại chưa
     */
    public CheckPhoneResponse checkPhone(String phone) {
        User user = userRepository.findByPhone(phone).orElse(null);

        if (user != null) {
            log.info("Phone {} exists with role {}", phone, user.getRole());
            return CheckPhoneResponse.builder()
                    .exists(true)
                    .role(user.getRole())
                    .fullName(user.getFullName())
                    .build();
        } else {
            log.info("Phone {} is new", phone);
            return CheckPhoneResponse.builder()
                    .exists(false)
                    .build();
        }
    }

    /**
     * Gửi OTP tới số điện thoại
     */
    public void sendOTP(String phone) {
        // Validate phone format (đã validate ở DTO level)

        // Generate and send OTP
        otpService.generateAndSaveOTP(phone);

        log.info("OTP sent to {}", phone);
    }

    /**
     * Verify OTP và đăng nhập/đăng ký
     */
    public AuthResponse verifyOTP(VerifyOTPRequest request) {
        // Verify OTP
        boolean isValid = otpService.verifyOTP(request.getPhone(), request.getOtp());

        // if (!isValid) {
        // throw new BadRequestException("Invalid or expired OTP");
        // }

        // Check if user exists
        User user = userRepository.findByPhone(request.getPhone())
                .orElse(null);

        boolean isNewUser = false;

        if (user == null) {
            // Đăng ký user mới
            user = User.builder()
                    .phone(request.getPhone())
                    .fullName(request.getFullName())
                    .role(request.getRole())
                    .build();

            user = userRepository.save(user);
            isNewUser = true;

            if (request.getRole().equals(UserRole.PROVIDER)) {
                // Tạo provider record tương ứng
                providerRepository.save(Provider.builder()
                        .id(user.getId())
                        .build());
            }

            log.info("✅ New user registered: {} ({})", user.getPhone(), user.getRole());
        } else {
            // User đã tồn tại - chỉ update fullName nếu có
            // if (request.getFullName() != null && !request.getFullName().isBlank()) {
            // user.setFullName(request.getFullName());
            // userRepository.save(user);
            // }

            log.info("✅ User logged in: {} ({})", user.getPhone(), user.getRole());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(UserDTO.from(user))
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * Resend OTP
     */
    public void resendOTP(String phone) {
        otpService.resendOTP(phone);
        log.info("OTP resent to {}", phone);
    }
}