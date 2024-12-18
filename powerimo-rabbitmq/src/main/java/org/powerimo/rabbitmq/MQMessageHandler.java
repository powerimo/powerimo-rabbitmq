package org.powerimo.rabbitmq;

public interface MQMessageHandler {
    void handleMessage(MQMessage message);
    void setUnsupportedCommandHandler(MQCommandHandler handler);
    void addCommandHandler(MQMessageType typeMessage, String commandName, MQCommandHandler commandHandler);
    void setInterceptor(MQCommandHandler handler);
    void setExceptionHandler(MQCommandExceptionHandler handler);
    MQExceptionResolution handleException(MQMessage message, Throwable ex);
}
