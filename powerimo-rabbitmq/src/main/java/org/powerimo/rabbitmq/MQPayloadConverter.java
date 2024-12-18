package org.powerimo.rabbitmq;

public interface MQPayloadConverter {
    <T> T extractPayload(MQMessage message, Class<T> cls);
    byte[] serializePayload(Object obj);
}
