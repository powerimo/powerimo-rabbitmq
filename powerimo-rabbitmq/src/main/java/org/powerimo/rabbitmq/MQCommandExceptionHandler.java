package org.powerimo.rabbitmq;

public interface MQCommandExceptionHandler {
    MQExceptionResolution handleException(MQMessage message, Throwable ex);
}
