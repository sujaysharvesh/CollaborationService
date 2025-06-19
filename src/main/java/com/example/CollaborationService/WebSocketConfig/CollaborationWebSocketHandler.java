package com.example.CollaborationService.WebSocketConfig;

import com.example.CollaborationService.DTO.UserPresenceMessage;
import com.example.CollaborationService.DTO.YWebSocketMessage;
import com.example.CollaborationService.Service.DocumentSaveService;
import com.example.CollaborationService.SessionServices.DocumentStateService;
import com.example.CollaborationService.SessionServices.UserSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

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
        return session.getUri().getPath().split("/")[2];
    }

    private String extractUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String documentId = extractDocumentId(session);
        String userId = extractUserId(session);

        documentSessions.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet()).add(session);
        userSessionService.addUserToDocument(documentId, userId, session.getId());
        byte[] currentState = documentStateService.getDocumentState(documentId);
        if(currentState != null) {
            session.sendMessage(new BinaryMessage(currentState));
        }
        broadcastUserJoined(documentId, userId, session);
    }

    @SneakyThrows
    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String documentId = extractDocumentId(session);
        String userId = extractUserId(session);
        byte[] payload = message.getPayload().array();
        YWebSocketMessage ywsMessage = YWebSocketMessage.parse(payload);

        switch (ywsMessage.getType()) {
            case SYNC_STEP_1:
                handleSyncStep1(session, documentId, ywsMessage);
                break;
            case SYNC_STEP_2:
                handleSyncStep2(session, documentId, ywsMessage);
                break;
            case UPDATE:
                handleUpdate(session, documentId, userId, ywsMessage);
                break;
            case AWARENESS:
                handleAwareness(session, documentId, ywsMessage);
                break;
        }
    }

    private void handleSyncStep1(WebSocketSession session,
                                 String documentId,
                                 YWebSocketMessage ywsMessage) throws IOException {
        byte[] currentState = documentStateService.getDocumentState(documentId);
        if ( currentState != null) {
            YWebSocketMessage syncResponse = new YWebSocketMessage();
            syncResponse.setType(YWebSocketMessage.Type.SYNC_STEP_1);
            syncResponse.setPayload(currentState);
            session.sendMessage(new BinaryMessage(syncResponse.toBytes()));
        } else  {
            YWebSocketMessage syncResponse = new YWebSocketMessage();
            syncResponse.setType(YWebSocketMessage.Type.SYNC_STEP_1);
            syncResponse.setPayload(new byte[0]);
            session.sendMessage(new BinaryMessage(syncResponse.toBytes()));
        }
    }

    private void handleSyncStep2(WebSocketSession session,
                                 String documentId,
                                 YWebSocketMessage ywsMessage) throws IOException {
        if (ywsMessage.getPayload().length > 0) {
            documentStateService.applyUpdate(documentId, ywsMessage.getPayload());
            Set<WebSocketSession> sessions = documentSessions.get(documentId);
            if (sessions != null) {
                for (WebSocketSession otherSession : sessions) {
                    if (!otherSession.getId().equals(session.getId()) && otherSession.isOpen()) {
                        try {
                            YWebSocketMessage updatedMessage = new YWebSocketMessage();
                            updatedMessage.setType(YWebSocketMessage.Type.SYNC_STEP_2);
                            updatedMessage.setPayload(ywsMessage.getPayload());
                            otherSession.sendMessage(new BinaryMessage(updatedMessage.toBytes()));
                        } catch (Exception e) {
                            sessions.remove(otherSession);
                        }
                    }
                }
            }
        }
    }

    private void handleAwareness(WebSocketSession session, String documentId, YWebSocketMessage ywsMessage) {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            for (WebSocketSession otherSession : sessions) {
                if (!otherSession.getId().equals(session.getId()) && otherSession.isOpen()) {
                    try {
                        otherSession.sendMessage(new BinaryMessage(ywsMessage.toBytes()));
                    } catch (Exception e) {
                        sessions.remove(otherSession);
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws JsonProcessingException {
        String documentId = extractDocumentId(session);
        String userId = extractUserId(session);

        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                documentSessions.remove(documentId);
            }
        }
        userSessionService.removeUserFromDocument(documentId, userId, session.getId());
        broadcastUserLeft(documentId, userId);
    }

    private void handleUpdate(WebSocketSession session,
                              String documentId,
                              String userId,
                              YWebSocketMessage yWebSocketMessage) throws IOException {
        documentStateService.applyUpdate(documentId, yWebSocketMessage.getPayload());
        Set<WebSocketSession> sessions = documentSessions.get(documentId);

        if (sessions != null) {
            for (WebSocketSession otherSession : sessions) {
                if (!otherSession.getId().equals(session.getId()) && otherSession.isOpen()) {
                    otherSession.sendMessage(new BinaryMessage(yWebSocketMessage.toBytes()));
                }
            }
        }

        documentStateService.updateLastModified(documentId, userId);
    }

    private void broadcastUserJoined(String documentId, String userId, WebSocketSession newSession) throws JsonProcessingException {
        UserPresenceMessage presenceMessage = new UserPresenceMessage();
        presenceMessage.setType(UserPresenceMessage.Type.USER_JOINED);
        presenceMessage.setUserId(userId);
        presenceMessage.setDocumentId(documentId);
        presenceMessage.setTimestamp(Instant.now());


        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            byte[] messageBytes = presenceMessage.toBytes();
            for (WebSocketSession session : sessions) {
                // Don't send to the user who just joined
                if (!session.getId().equals(newSession.getId()) && session.isOpen()) {
                    try {
                        session.sendMessage(new BinaryMessage(messageBytes));
                    } catch (Exception e) {
                        sessions.remove(session);
                    }
                }
            }
        }
    }

    private void broadcastUserLeft(String documentId, String userId) throws JsonProcessingException {
        UserPresenceMessage leftMessage = new UserPresenceMessage();
        leftMessage.setType(UserPresenceMessage.Type.USER_LEFT);
        leftMessage.setUserId(userId);
        leftMessage.setDocumentId(documentId);
        leftMessage.setTimestamp(Instant.now());
        Set<WebSocketSession> sessions = documentSessions.get(documentId);

        if (sessions != null) {
            byte[] messageBytes = leftMessage.toBytes();
            for (WebSocketSession session: sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new BinaryMessage(messageBytes));
                    } catch (Exception e) {
                        sessions.remove(session);
                    }
                }
            }
        }
    }


}
