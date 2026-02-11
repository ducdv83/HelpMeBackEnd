package com.helpme.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String OTP_PREFIX = "otp:";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 5;

    /**
     * Tạo và lưu OTP vào Redis
     */
    public String generateAndSaveOTP(String phone) {
        // Generate random 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save to Redis with TTL 5 minutes
        String key = OTP_PREFIX + phone;
        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("✅ Generated OTP for {}: {} (expires in {} minutes)",
                phone, otp, OTP_EXPIRATION_MINUTES);

        // TODO: Integrate SMS service (Twilio, Firebase, VNPT, etc.)
        // For now, just log it

        return otp;
    }

    /**
     * Verify OTP
     */
    public boolean verifyOTP(String phone, String otp) {
        String key = OTP_PREFIX + phone;
        String storedOTP = (String) redisTemplate.opsForValue().get(key);

        if (storedOTP != null && storedOTP.equals(otp)) {
            // Delete OTP after successful verification
            redisTemplate.delete(key);
            log.info("✅ OTP verified successfully for {}", phone);
            return true;
        }

        log.warn("❌ Invalid OTP for {}: provided={}, stored={}", phone, otp, storedOTP);
        return false;
    }

    /**
     * Resend OTP (generate new one)
     */
    public String resendOTP(String phone) {
        // Delete old OTP
        redisTemplate.delete(OTP_PREFIX + phone);

        // Generate new OTP
        return generateAndSaveOTP(phone);
    }
}