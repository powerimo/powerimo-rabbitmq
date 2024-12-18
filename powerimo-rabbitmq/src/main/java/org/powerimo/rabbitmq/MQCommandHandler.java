package org.powerimo.rabbitmq;

public interface MQCommandHandler {
    void handleMessage(MQMessage message);
}
