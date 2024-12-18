package org.powerimo.rabbitmq;

public interface CommandHandler {
    void handleMessage(Message message);
}
