package com.example.CollaborationService.WebSocketConfig;

import com.example.CollaborationService.DTO.UserPresenceMessage;
import com.example.CollaborationService.Service.DocumentSaveService;
import com.example.CollaborationService.SessionServices.DocumentStateService;
import com.example.CollaborationService.SessionServices.UserSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CollaborationWebSocketHandler extends BinaryWebSocketHandler {

    private final DocumentStateService documentStateService;
    private final DocumentSaveService documentSaveService;
    private final UserSessionService userSessionService;

    @Autowired
    public CollaborationWebSocketHandler(DocumentSaveService documentSaveService,
                                         DocumentStateService documentStateService,
                                         UserSessionService userSessionService) {
        this.documentSaveService = documentSaveService;
        this.documentStateService = documentStateService;
        this.userSessionService = userSessionService;
    }

    private final Map<String, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();

    private String extractDocumentId(WebSocketSession session) {
        try {
            String fullPath = session.getUri().getPath();
            log.info("Full WebSocket path: {}", fullPath);

            // Extract document ID from path like: /ws/collaboration/documentId
            String[] parts = fullPath.split("/");
            log.info("Path parts: {}", String.join(", ", parts));

            if (parts.length >= 3) {
                String documentId = parts[3]; // /ws/collaboration/documentId
                log.info("Extracted documentId: {}", documentId);
                return documentId;
            } else {
                log.error("Invalid path format: {}", fullPath);
                return null;
            }
        } catch (Exception e) {
            log.error("Error extracting document ID from path: {}", session.getUri().getPath(), e);
            return null;
        }
    }

    private String extractUserId(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        log.info("Extracted userId from session attributes: {}", userId);
        return userId;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String documentId = null;
        String userId = null;

        try {
            log.info("=== WebSocket Connection Established ===");
            log.info("Session ID: {}", session.getId());
            log.info("Session URI: {}", session.getUri());
            log.info("Session attributes: {}", session.getAttributes());

            documentId = extractDocumentId(session);
            userId = extractUserId(session);

            if (documentId == null) {
                log.error("DocumentId is null - cannot proceed");
                session.close(CloseStatus.BAD_DATA.withReason("Missing documentId"));
                return;
            }

            if (userId == null) {
                log.error("UserId is null - cannot proceed");
                session.close(CloseStatus.BAD_DATA.withReason("Missing userId"));
                return;
            }

            log.info("WebSocket connected - DocumentId: {}, UserId: {}", documentId, userId);

            // Add session to document sessions
            log.info("Adding session to document sessions...");
            documentSessions.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Session added to document sessions. Total sessions for document {}: {}",
                    documentId, documentSessions.get(documentId).size());

            // Add user to document in session service
            log.info("Adding user to document in UserSessionService...");
            userSessionService.addUserToDocument(documentId, userId, session.getId());
            log.info("User added to document successfully");

            // Send current document state
            log.info("Retrieving current document state...");
            byte[] currentState = null;
            try {
                currentState = documentStateService.getDocumentState(documentId);
                log.info("Document state retrieved. Size: {}",
                        currentState != null ? currentState.length : "null");
            } catch (Exception e) {
                log.error("Error retrieving document state for documentId: {}", documentId, e);
                // Don't fail the connection for this - just log and continue
            }

            if (currentState != null && currentState.length > 0) {
                log.info("Sending current document state to client...");
                try {
                    session.sendMessage(new BinaryMessage(currentState));
                    log.info("Document state sent successfully");
                } catch (Exception e) {
                    log.error("Error sending document state to client", e);
                    throw e; // This might be the source of your exception
                }
            } else {
                log.info("No document state to send (null or empty)");
            }

            // Broadcast user joined
            log.info("Broadcasting user joined...");
            try {
                broadcastUserJoined(documentId, userId, session);
                log.info("User joined broadcast completed");
            } catch (Exception e) {
                log.error("Error broadcasting user joined", e);
                // Don't fail the connection for broadcast issues
            }

            log.info("=== WebSocket Connection Setup Complete ===");

        } catch (Exception e) {
            log.error("CRITICAL ERROR in afterConnectionEstablished for documentId: {}, userId: {}",
                    documentId, userId, e);
            e.printStackTrace(); // Print full stack trace
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Connection setup failed: " + e.getMessage()));
            } catch (IOException ioException) {
                log.error("Error closing session after setup failure", ioException);
            }
            throw e; // Re-throw to ensure the error is properly handled
        }
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String documentId = null;
        String userId = null;

        try {
            documentId = extractDocumentId(session);
            userId = extractUserId(session);
            byte[] payload = message.getPayload().array();

            log.info("Received binary message - DocumentId: {}, UserId: {}, Size: {}",
                    documentId, userId, payload.length);

            if (documentId == null || userId == null) {
                log.warn("Received message with missing documentId or userId");
                return;
            }

            handleYjsMessage(session, documentId, userId, payload);

        } catch (Exception e) {
            log.error("Error processing binary message for documentId: {}, userId: {}",
                    documentId, userId, e);
            e.printStackTrace();
            // Don't close the session for message processing errors
        }
    }

    private void handleYjsMessage(WebSocketSession session,
                                  String documentId,
                                  String userId,
                                  byte[] payload) throws IOException {

        try {
            log.debug("Applying Yjs update to document state...");
            documentStateService.applyUpdate(documentId, payload);
            log.debug("Yjs update applied successfully");
        } catch (Exception e) {
            log.error("Error applying Yjs update", e);
            throw e;
        }

        // Broadcast to other sessions
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            log.debug("Broadcasting to {} sessions", sessions.size());
            sessions.removeIf(otherSession -> {
                if (!otherSession.getId().equals(session.getId()) && otherSession.isOpen()) {
                    try {
                        otherSession.sendMessage(new BinaryMessage(payload));
                        return false; // Keep session
                    } catch (Exception e) {
                        log.warn("Failed to send message to session {}, removing from document {}",
                                otherSession.getId(), documentId, e);
                        return true; // Remove session
                    }
                } else if (!otherSession.isOpen()) {
                    log.debug("Removing closed session {} from document {}",
                            otherSession.getId(), documentId);
                    return true; // Remove closed session
                }
                return false; // Keep session (it's the sender)
            });
        }

        // Update last modified
        try {
            documentStateService.updateLastModified(documentId, userId);
        } catch (Exception e) {
            log.error("Error updating last modified", e);
            // Don't fail for this
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            String documentId = extractDocumentId(session);
            String userId = extractUserId(session);

            log.info("WebSocket disconnected - DocumentId: {}, UserId: {}, Status: {}, Reason: {}",
                    documentId, userId, status.getCode(), status.getReason());

            if (documentId != null) {
                Set<WebSocketSession> sessions = documentSessions.get(documentId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        documentSessions.remove(documentId);
                        log.info("Removed empty document session for documentId: {}", documentId);
                    }
                }
            }

            if (userId != null && documentId != null) {
                try {
                    userSessionService.removeUserFromDocument(documentId, userId, session.getId());
                    broadcastUserLeft(documentId, userId);
                } catch (Exception e) {
                    log.error("Error during user cleanup", e);
                }
            }

        } catch (Exception e) {
            log.error("Error in afterConnectionClosed", e);
        }
    }

    private void broadcastUserJoined(String documentId, String userId, WebSocketSession newSession) {
        try {
            log.debug("Creating user joined presence message...");
            UserPresenceMessage presenceMessage = new UserPresenceMessage();
            presenceMessage.setType(UserPresenceMessage.Type.USER_JOINED);
            presenceMessage.setUserId(userId);
            presenceMessage.setDocumentId(documentId);
            presenceMessage.setTimestamp(Instant.now());

            Set<WebSocketSession> sessions = documentSessions.get(documentId);
            if (sessions != null && sessions.size() > 1) { // Only broadcast if there are other users
                byte[] messageBytes = presenceMessage.toBytes();
                log.debug("Broadcasting user joined to {} other sessions", sessions.size() - 1);

                sessions.removeIf(session -> {
                    if (!session.getId().equals(newSession.getId()) && session.isOpen()) {
                        try {
                            session.sendMessage(new BinaryMessage(messageBytes));
                            return false; // Keep session
                        } catch (Exception e) {
                            log.warn("Failed to broadcast user joined to session {}", session.getId(), e);
                            return true; // Remove session
                        }
                    }
                    return false; // Keep session
                });
            } else {
                log.debug("No other sessions to broadcast to");
            }
        } catch (Exception e) {
            log.error("Error broadcasting user joined for documentId: {}, userId: {}", documentId, userId, e);
        }
    }

    private void broadcastUserLeft(String documentId, String userId) {
        try {
            UserPresenceMessage leftMessage = new UserPresenceMessage();
            leftMessage.setType(UserPresenceMessage.Type.USER_LEFT);
            leftMessage.setUserId(userId);
            leftMessage.setDocumentId(documentId);
            leftMessage.setTimestamp(Instant.now());

            Set<WebSocketSession> sessions = documentSessions.get(documentId);
            if (sessions != null && !sessions.isEmpty()) {
                byte[] messageBytes = leftMessage.toBytes();
                sessions.removeIf(session -> {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new BinaryMessage(messageBytes));
                            return false; // Keep session
                        } catch (Exception e) {
                            log.warn("Failed to broadcast user left to session {}", session.getId(), e);
                            return true; // Remove session
                        }
                    } else {
                        return true; // Remove closed session
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error broadcasting user left for documentId: {}, userId: {}", documentId, userId, e);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}