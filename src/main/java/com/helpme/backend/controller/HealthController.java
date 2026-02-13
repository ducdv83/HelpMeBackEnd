// controller/HealthController.java
package com.helpme.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/health")
public class HealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean redisEnabled;

    // ✅ Constructor with optional RedisTemplate
    public HealthController(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.data.redis.enabled:false}") boolean redisEnabled) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
    }

    /**
     * Health check endpoint
     * GET /v1/health
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("service", "helpme-backend");
        health.put("timestamp", System.currentTimeMillis());

        // Check Redis status
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                health.put("redis", "UP");
                log.debug("✅ Redis health check: UP");
            } catch (Exception e) {
                health.put("redis", "DOWN");
                health.put("redis_error", e.getMessage());
                log.warn("⚠️ Redis health check: DOWN - {}", e.getMessage());
            }
        } else {
            health.put("redis", "DISABLED");
            log.debug("📦 Redis health check: DISABLED");
        }

        // Check Database (optional - add if you have DataSource)
        // health.put("database", checkDatabaseHealth());

        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check (for monitoring)
     * GET /v1/health/detailed
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("service", "helpme-backend");
        health.put("timestamp", System.currentTimeMillis());
        health.put("redis_enabled", redisEnabled);

        // Redis details
        Map<String, Object> redisHealth = new HashMap<>();
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                redisHealth.put("status", "UP");
                redisHealth.put("enabled", true);

                // Get Redis info (optional)
                try {
                    Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
                    redisHealth.put("keys_count", dbSize);
                } catch (Exception e) {
                    log.debug("Could not get Redis dbSize: {}", e.getMessage());
                }

            } catch (Exception e) {
                redisHealth.put("status", "DOWN");
                redisHealth.put("error", e.getMessage());
                redisHealth.put("enabled", true);
            }
        } else {
            redisHealth.put("status", "DISABLED");
            redisHealth.put("enabled", false);
        }
        health.put("redis", redisHealth);

        // System info
        Map<String, Object> system = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        system.put("processors", runtime.availableProcessors());
        system.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
        system.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
        system.put("max_memory_mb", runtime.maxMemory() / 1024 / 1024);
        health.put("system", system);

        return ResponseEntity.ok(health);
    }
}