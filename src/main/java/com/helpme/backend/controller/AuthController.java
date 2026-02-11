package com.helpme.backend.controller;

import com.helpme.backend.dto.*;
import com.helpme.backend.entity.User;
import com.helpme.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/check-phone
     * Kiểm tra số điện thoại đã tồn tại chưa
     */
    @PostMapping("/check-phone")
    public ResponseEntity<CheckPhoneResponse> checkPhone(
            @RequestBody @Valid SendOTPRequest request) {
        CheckPhoneResponse response = authService.checkPhone(request.getPhone());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /v1/auth/send-otp
     * Gửi OTP đến số điện thoại
     */
    @PostMapping("/send-otp")
    public ResponseEntity<MessageResponse> sendOTP(
            @RequestBody @Valid SendOTPRequest request) {
        authService.sendOTP(request.getPhone());
        return ResponseEntity.ok(new MessageResponse("OTP sent successfully"));
    }

    /**
     * POST /v1/auth/verify-otp
     * Verify OTP và đăng nhập/đăng ký
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOTP(
            @RequestBody @Valid VerifyOTPRequest request) {
        AuthResponse response = authService.verifyOTP(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /v1/auth/resend-otp
     * Gửi lại OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOTP(
            @RequestBody @Valid SendOTPRequest request) {
        authService.resendOTP(request.getPhone());
        return ResponseEntity.ok(new MessageResponse("OTP resent successfully"));
    }

    /**
     * GET /v1/auth/me
     * Lấy thông tin user hiện tại (cần authentication)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserDTO.from(currentUser));
    }
}