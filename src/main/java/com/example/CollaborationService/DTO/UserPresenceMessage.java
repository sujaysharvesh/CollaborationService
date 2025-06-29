package com.example.CollaborationService.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Setter
@Getter
public class UserPresenceMessage {

    public enum Type {
        USER_JOINED((byte) 10),
        USER_LEFT((byte) 11);

        private final byte value;
        Type(byte value) {
            this.value = value;
        }
    }
    private Type type;
    private String userId;
    private String documentId;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant timestamp;

    public byte[] toBytes() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsBytes(this);
    }
}
