package com.example.CollaborationService.DTO;

import com.thoughtworks.xstream.converters.time.YearConverter;

import java.lang.reflect.Array;
import java.util.Arrays;

public class YWebSocketMessage {
    public enum Type {
        SYNC_STEP_1((byte) 0),
        SYNC_STEP_2((byte) 1),
        UPDATE((byte) 2),
        AWARENESS((byte) 3);

        private final byte values;
        Type(byte values) {
            this.values = values;
        }

        public byte getValue() {
            return values;
        }
    }
    private Type type;
    private byte[] payload;

    public static YWebSocketMessage parse(byte[] data) {
        YWebSocketMessage message = new YWebSocketMessage();
        message.type = Type.values()[data[0]];
        message.payload = Arrays.copyOfRange(data, 1, data.length);
        return message;
    }

    public byte[] toBytes() {
        byte[] result = new byte[payload.length + 1];
        result[0] = type.getValue();
        System.arraycopy(payload, 0 , result, 1, payload.length);
        return result;
    }

    public Type getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }

}
