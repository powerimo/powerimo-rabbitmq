package org.powerimo.rabbitmq;

public interface RabbitQueueListener {
    void start();
    void stop();
    ServiceStatus getStatus();
}
