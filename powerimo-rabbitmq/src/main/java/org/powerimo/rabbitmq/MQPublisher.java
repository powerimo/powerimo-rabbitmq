package org.powerimo.rabbitmq;

public interface MQPublisher {
    MQMessage sendMessage(MQMessage message, String exchangeName, String routingKey);
    MQMessage sendEvent(String name, Object payload);
    MQMessage sendTask(String name, Object payload, String routingKey);
    MQMessage sendTaskResult(String name, Object payload, Integer resultCode, String resultMessage, MQMessage sourceMessage);
    void setConverter(MQPayloadConverter converter);
}
