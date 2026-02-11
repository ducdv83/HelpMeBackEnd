package com.helpme.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    /**
     * Gửi push notification qua Expo
     */
    public void sendPushNotification(String pushToken, String title, String body) {
        if (pushToken == null || pushToken.isBlank()) {
            log.warn("⚠️ Push token is empty, skipping notification");
            return;
        }

        try {
            Map<String, Object> message = Map.of(
                    "to", pushToken,
                    "title", title,
                    "body", body,
                    "sound", "default",
                    "priority", "high",
                    "channelId", "default");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            String response = restTemplate.postForObject(EXPO_PUSH_URL, request, String.class);

            log.info("📲 Push notification sent to {}: {}", pushToken, response);
        } catch (Exception e) {
            log.error("❌ Failed to send push notification to {}", pushToken, e);
        }
    }

    /**
     * Gửi bulk push notifications
     */
    public void sendBulkNotifications(List<String> pushTokens, String title, String body) {
        pushTokens.forEach(token -> sendPushNotification(token, title, body));
    }
}