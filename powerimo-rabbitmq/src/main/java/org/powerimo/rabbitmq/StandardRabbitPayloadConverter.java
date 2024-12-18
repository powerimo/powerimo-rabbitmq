package org.powerimo.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class StandardRabbitPayloadConverter implements RabbitPayloadConverter {
    private final ObjectMapper objectMapper;

    public <T> T extractPayload(Message message, Class<T> cls) {
        if (message == null)
            throw new RabbitException("message is null");
        if (message.getPayload() == null)
            return null;
        if (cls.isInstance(message.getPayload()))
            return (T) message.getPayload();

        try {
            if (message.getPayload() instanceof String) {
                return objectMapper.readValue((String) message.getPayload(), cls);
            }
        } catch (JsonProcessingException ex) {
            throw new RabbitException("Exception on converting JSON to payload class", ex);
        }

        try {
            var aCls = byte[].class;
            if (aCls.isInstance(message.getPayload())) {
                byte[] arr = (byte[]) message.getPayload();
                return objectMapper.readValue(arr, cls);
            }
        } catch (Exception e) {
            throw new RabbitException("Exception on converting JSON to payload class", e);
        }
        throw new RabbitException("Couldn't extract payload as class " + cls.getName());
    }

    public byte[] serializePayload(Object obj) {
        if (obj == null)
            return null;
        try {
            String s = objectMapper.writeValueAsString(obj);
            return s.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException ex) {
            throw new RabbitException("Exception on serialization payload", ex);
        }
    }
}
