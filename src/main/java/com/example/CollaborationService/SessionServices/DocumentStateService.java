package com.example.CollaborationService.SessionServices;


import com.example.CollaborationService.DTO.DocumentMetadata;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Service
public class DocumentStateService {

    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String DOCUMENT_STATE_KEY = "doc:state:";
    private static final String DOCUMENT_META_KEY = "doc:meta:";

    public byte[] getDocumentState(String documentId) {
        return redisTemplate.opsForValue().get(DOCUMENT_STATE_KEY + documentId);
    }

    public void applyUpdate(String documentId, byte[] updates) {
        String key = DOCUMENT_STATE_KEY + documentId;
        byte[] currentState = redisTemplate.opsForValue().get(key);
        byte[] newState = mergeYjsUpdate(currentState, updates);
        redisTemplate.opsForValue().set(key, newState);
        redisTemplate.expire(key, Duration.ofHours(24));

    }

    public byte[] mergeYjsUpdate(byte[] currentState, byte[] updates) {
        if (currentState == null){
            return updates;
        }
        byte[] result = new byte[currentState.length + updates.length];
        System.arraycopy(currentState, 0, result, 0, currentState.length);
        System.arraycopy(updates, 0, result, currentState.length, updates.length);
        return result;
    }

    public void updateLastModified(String documentId, String userId) throws IOException {
        DocumentMetadata metadata = getDocumentMetadata(documentId);
        metadata.setLastModified(Instant.now());
        metadata.setLastModifiedBy(userId);
        redisTemplate.opsForValue()
                .set(DOCUMENT_META_KEY + documentId, metadata.toBytes(), Duration.ofHours(24));
    }

    private DocumentMetadata getDocumentMetadata(String documentId) throws IOException {
        byte[] data = redisTemplate.opsForValue().get(DOCUMENT_META_KEY + documentId);
        return data != null ? DocumentMetadata.fromBytes(data) : new DocumentMetadata(documentId);
    }
}
