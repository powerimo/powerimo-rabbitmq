package org.powerimo.rabbitmq;

public interface RabbitMessageHandler {
    void handleMessage(Message message);
    void setUnsupportedCommandHandler(CommandHandler handler);
    void addCommandHandler(MessageType typeMessage, String commandName, CommandHandler commandHandler);
    void setInterceptor(CommandHandler handler);
    void setExceptionHandler(CommandExceptionHandler handler);
    ExceptionResolution handleException(Message message, Throwable ex);
}
