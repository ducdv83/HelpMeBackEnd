package com.helpme.backend.controller;

import com.helpme.backend.websocket.SocketIOService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SocketIOService socketService;

    /**
     * GET /v1/health
     * Health check endpoint
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // Check Redis
        try {
            redisTemplate.opsForValue().get("health-check");
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN");
        }

        // Check Socket.IO
        health.put("socketio", Map.of(
                "status", "UP",
                "onlineUsers", socketService.getOnlineUserCount()));

        return ResponseEntity.ok(health);
    }
}