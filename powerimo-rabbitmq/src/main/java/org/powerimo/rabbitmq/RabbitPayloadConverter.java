package org.powerimo.rabbitmq;

public interface RabbitPayloadConverter {
    <T> T extractPayload(Message message, Class<T> cls);
    byte[] serializePayload(Object obj);
}
