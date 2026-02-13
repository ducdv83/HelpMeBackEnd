// service/OTPService.java
package com.helpme.backend.service;

import com.helpme.backend.entity.Otp;
import com.helpme.backend.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OTPService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OtpRepository otpRepository;
    private final boolean redisEnabled;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final String OTP_KEY_PREFIX = "otp:";

    // ✅ Constructor with optional RedisTemplate
    public OTPService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            OtpRepository otpRepository,
            @Value("${spring.data.redis.enabled:false}") boolean redisEnabled) {
        this.redisTemplate = redisTemplate;
        this.otpRepository = otpRepository;
        this.redisEnabled = redisEnabled;

        log.info("🔧 OTPService initialized with Redis: {}", redisEnabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Generate and save OTP (with fallback)
     */
    public String generateAndSaveOTP(String phone) {
        String otp = generateOTP();

        if (redisEnabled && redisTemplate != null) {
            saveOTPInRedis(phone, otp);
        } else {
            log.info("📦 Using Database for OTP storage (Redis disabled)");
            saveOTPInDatabase(phone, otp);
        }

        return otp;
    }

    /**
     * Verify OTP (with fallback)
     */
    public boolean verifyOTP(String phone, String otp) {
        if (redisEnabled && redisTemplate != null) {
            return verifyOTPFromRedis(phone, otp);
        } else {
            return verifyOTPFromDatabase(phone, otp);
        }
    }

    /**
     * Resend OTP (with fallback)
     */
    public String resendOTP(String phone) {
        return generateAndSaveOTP(phone);
    }

    // ==================== PRIVATE METHODS ====================

    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Redis methods
    private void saveOTPInRedis(String phone, String otp) {
        try {
            String key = OTP_KEY_PREFIX + phone;
            redisTemplate.opsForValue().set(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
            log.info("✅ Redis: OTP saved for phone {} (expires in {} min)", phone, OTP_EXPIRY_MINUTES);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to save OTP for {}: {}", phone, e.getMessage());
            log.warn("⚠️ Falling back to database storage");
            saveOTPInDatabase(phone, otp);
        }
    }

    private boolean verifyOTPFromRedis(String phone, String otp) {
        try {
            String key = OTP_KEY_PREFIX + phone;
            String storedOTP = (String) redisTemplate.opsForValue().get(key);

            if (storedOTP != null && storedOTP.equals(otp)) {
                redisTemplate.delete(key);
                log.info("✅ Redis: OTP verified for phone {}", phone);
                return true;
            }

            log.warn("⚠️ Redis: Invalid OTP for phone {}", phone);
            return false;
        } catch (Exception e) {
            log.error("❌ Redis: Failed to verify OTP for {}: {}", phone, e.getMessage());
            log.warn("⚠️ Falling back to database verification");
            return verifyOTPFromDatabase(phone, otp);
        }
    }

    // Database methods
    private void saveOTPInDatabase(String phone, String otp) {
        try {
            // Delete old OTPs for this phone
            otpRepository.deleteByPhone(phone);

            // Create new OTP
            Otp otpEntity = new Otp();
            otpEntity.setPhone(phone);
            otpEntity.setCode(otp);
            otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpRepository.save(otpEntity);

            log.info("✅ Database: OTP saved for phone {} (expires in {} min)", phone, OTP_EXPIRY_MINUTES);
        } catch (Exception e) {
            log.error("❌ Database: Failed to save OTP for {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to save OTP", e);
        }
    }

    private boolean verifyOTPFromDatabase(String phone, String otp) {
        try {
            Optional<Otp> otpEntity = otpRepository.findByPhoneAndCode(phone, otp);

            if (otpEntity.isPresent() && otpEntity.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                otpRepository.delete(otpEntity.get());
                log.info("✅ Database: OTP verified for phone {}", phone);
                return true;
            }

            log.warn("⚠️ Database: Invalid or expired OTP for phone {}", phone);
            return false;
        } catch (Exception e) {
            log.error("❌ Database: Failed to verify OTP for {}: {}", phone, e.getMessage());
            return false;
        }
    }
}