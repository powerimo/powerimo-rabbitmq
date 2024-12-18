package org.powerimo.rabbitmq;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

@Slf4j
public class StandardRabbitMessageHandler implements RabbitMessageHandler {
    private final LinkedHashMap<String, CommandHandler> eventHandlers = new LinkedHashMap<>();
    private final LinkedHashMap<String, CommandHandler> taskHandlers = new LinkedHashMap<>();
    private final LinkedHashMap<String, CommandHandler> taskResultHandlers = new LinkedHashMap<>();
    private CommandHandler unsupportedHandler;
    private CommandHandler interceptor;
    private CommandExceptionHandler exceptionHandler;

    public StandardRabbitMessageHandler() {

    }

    @Override
    public void handleMessage(@NonNull Message message) {
        log.debug("[MQ->]: {}", message);
        MessageType typeMQMessage = RabbitUtils.getTypeMessage(message);
        if (interceptor != null) {
            interceptor.handleMessage(message);
        }
        CommandHandler handler;
        switch (typeMQMessage) {
            case EVENT:
                handler = eventHandlers.get(message.getName().toLowerCase());
                // unknown events ignored
                if (handler == null) {
                    log.trace("ignore event: {}", message.getName());
                    return;
                }
                break;
            case TASK:
                handler = taskHandlers.get(message.getName().toLowerCase());
                break;
            case TASK_RESULT:
                handler = taskResultHandlers.get(message.getName().toLowerCase());
                break;
            default:
                handler = null;
        }

        // call handler
        if (handler != null) {
            log.trace("handler found: " + handler.getClass().getName());
            try {
                handler.handleMessage(message);
            } catch (Exception ex) {
                if (exceptionHandler != null) {
                    exceptionHandler.handleException(message, ex);
                } else {
                    throw ex;
                }
            }
        } else {
            // if unsupported handler is set, call it
            if (unsupportedHandler != null) {
                log.trace("passing message to unsupportedCommandHandler: {}", message);
                unsupportedHandler.handleMessage(message);
            } else {
                var errorMessage = "Unsupported message: type=" + typeMQMessage.name() + ", name="+ message.getName();
                throw new RabbitException(errorMessage);
            }
        }
    }

    @Override
    public void setUnsupportedCommandHandler(CommandHandler handler) {
        unsupportedHandler = handler;
        if (handler == null) {
            log.debug("Handler for unsupported commands was cleared");
        } else {
            log.debug("Handler for unsupported commands was set: {}", handler.getClass().getName());
        }
    }

    @Override
    public void addCommandHandler(@NonNull MessageType typeMessage, @NonNull String commandName, @NonNull CommandHandler commandHandler) {
        switch (typeMessage) {
            case EVENT:
                eventHandlers.putIfAbsent(commandName.toLowerCase(), commandHandler);
                break;
            case TASK:
                taskHandlers.putIfAbsent(commandName.toLowerCase(), commandHandler);
                break;
            case TASK_RESULT:
                taskResultHandlers.putIfAbsent(commandName.toLowerCase(), commandHandler);
                break;
            default:
                throw new RabbitException("Type is not supported for registering command handlers: " + typeMessage.name());
        }
    }

    @Override
    public void setInterceptor(CommandHandler handler) {
        interceptor = handler;
    }

    @Override
    public void setExceptionHandler(CommandExceptionHandler handler) {
        exceptionHandler = handler;
    }

    @Override
    public ExceptionResolution handleException(Message message, Throwable ex) {
        if (exceptionHandler != null) {
            return exceptionHandler.handleException(message, ex);
        } else {
            return ExceptionResolution.DEFAULT;
        }
    }
}
