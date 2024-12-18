package org.powerimo.rabbitmq;

public interface CommandExceptionHandler {
    ExceptionResolution handleException(Message message, Throwable ex);
}
