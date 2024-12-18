package org.powerimo.rabbitmq;

public interface RabbitMessagePublisher {
    Message sendMessage(Message message, String exchangeName, String routingKey);
    Message sendEvent(String name, Object payload);
    Message sendTask(String name, Object payload, String routingKey);
    Message sendTaskResult(String name, Object payload, Integer resultCode, String resultMessage, Message sourceMessage);
    void setConverter(RabbitPayloadConverter converter);
}
