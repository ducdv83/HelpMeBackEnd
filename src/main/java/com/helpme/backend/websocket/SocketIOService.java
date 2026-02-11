package com.helpme.backend.websocket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIOService {

    private final SocketIOServer server;

    // Map: userId -> sessionId
    private final Map<UUID, UUID> userSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        server.addConnectListener(client -> {
            String userIdParam = client.getHandshakeData().getSingleUrlParam("userId");

            if (userIdParam != null) {
                try {
                    UUID userId = UUID.fromString(userIdParam);
                    userSessions.put(userId, client.getSessionId());
                    log.info("✅ User {} connected with session {}", userId, client.getSessionId());
                } catch (IllegalArgumentException e) {
                    log.warn("❌ Invalid userId format: {}", userIdParam);
                }
            } else {
                log.warn("❌ Client connected without userId parameter");
            }
        });

        server.addDisconnectListener(client -> {
            // Remove from userSessions
            userSessions.entrySet().removeIf(entry -> entry.getValue().equals(client.getSessionId()));
            log.info("👋 Client {} disconnected", client.getSessionId());
        });

        server.start();
        log.info("✅ Socket.IO server started on {}:{}",
                server.getConfiguration().getHostname(),
                server.getConfiguration().getPort());
    }

    /**
     * Gửi event tới một user cụ thể
     */
    public void emitToUser(UUID userId, String event, Object data) {
        UUID sessionId = userSessions.get(userId);

        if (sessionId != null) {
            SocketIOClient client = server.getClient(sessionId);
            if (client != null) {
                client.sendEvent(event, data);
                log.debug("📤 Emitted event '{}' to user {}", event, userId);
            } else {
                log.warn("⚠️ Client not found for session {}", sessionId);
                userSessions.remove(userId);
            }
        } else {
            log.debug("⚠️ User {} not connected", userId);
        }
    }

    /**
     * Broadcast event tới nhiều users
     */
    public void broadcastToUsers(List<UUID> userIds, String event, Object data) {
        userIds.forEach(userId -> emitToUser(userId, event, data));
    }

    /**
     * Kiểm tra user có đang online không
     */
    public boolean isUserOnline(UUID userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * Lấy số lượng users đang online
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    @PreDestroy
    public void stop() {
        server.stop();
        log.info("👋 Socket.IO server stopped");
    }
}