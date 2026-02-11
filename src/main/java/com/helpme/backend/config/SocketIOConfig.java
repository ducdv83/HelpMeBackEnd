package com.helpme.backend.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.host}")
    private String host;

    @Value("${socketio.port}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();

        config.setHostname(host);
        config.setPort(port);

        // Performance tuning
        config.setPingInterval(25000); // 25 seconds
        config.setPingTimeout(60000); // 60 seconds
        config.setMaxFramePayloadLength(1024 * 1024); // 1MB
        config.setMaxHttpContentLength(1024 * 1024); // 1MB

        // CORS
        config.setOrigin("*");

        return new SocketIOServer(config);
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new SpringAnnotationScanner(socketServer);
    }
}