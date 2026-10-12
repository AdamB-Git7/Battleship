package com.adam.battleship.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Turns on WebSocket messaging with STOMP.
 *
 * Message flow:
 *   browser  --SEND /app/...-->        @MessageMapping method in a controller
 *   server   --/topic/... or /queue/-> every browser subscribed to that destination
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The URL the browser connects to: new WebSocket("ws://host/ws")
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Built-in in-memory broker. Delivers messages to subscribers of
        // /topic (shared, e.g. a room's public updates) and /queue (one player only).
        registry.enableSimpleBroker("/topic", "/queue");

        // Messages sent by the browser to /app/... are routed to @MessageMapping methods.
        registry.setApplicationDestinationPrefixes("/app");

        // Lets the server send to one specific player via /user/queue/...
        // (needed so each player only gets their own board, the anti-cheat rule).
        registry.setUserDestinationPrefix("/user");
    }
}
