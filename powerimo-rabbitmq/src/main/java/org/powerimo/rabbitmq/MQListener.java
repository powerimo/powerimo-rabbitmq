package org.powerimo.rabbitmq;

public interface MQListener {
    void start();
    void stop();
    ServiceStatus getStatus();
}
