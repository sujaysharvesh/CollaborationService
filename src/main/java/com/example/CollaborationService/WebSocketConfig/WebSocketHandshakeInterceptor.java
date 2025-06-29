package com.example.CollaborationService.WebSocketConfig;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // Extract query parameters from the request URI
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null) {
            Map<String, String> queryParams = UriComponentsBuilder.fromUriString("?" + query)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();

            // Extract user information from query parameters
            String userId = queryParams.get("userId");
            String userName = queryParams.get("userName");
            String userColor = queryParams.get("userColor"); // Fixed typo

            if (userId == null || userId.trim().isEmpty()) {
                System.err.println("Missing or empty userId in WebSocket handshake");
                return false; // Reject handshake
            }

            // Store user information in session attributes
            attributes.put("userId", userId);
            attributes.put("userName", userName != null ? userName : "Anonymous");
            attributes.put("userColor", userColor != null ? userColor : "#000000");

            System.out.println("WebSocket handshake - UserId: " + userId +
                    ", UserName: " + userName +
                    ", UserColor: " + userColor);
        } else {
            System.err.println("No query parameters found in WebSocket handshake");
            return false; // Reject handshake if no query params
        }

        return true; // Allow handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            System.err.println("WebSocket handshake failed: " + exception.getMessage());
        } else {
            System.out.println("WebSocket handshake completed successfully");
        }
    }
}