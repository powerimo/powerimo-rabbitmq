package org.powerimo.rabbitmq;

public class RabbitException extends RuntimeException {
    public RabbitException() {
        super();
    }

    public RabbitException(String message) {
        super(message);
    }

    public RabbitException(String message, Throwable cause) {
        super(message, cause);
    }

    public RabbitException(Throwable cause) {
        super(cause);
    }
}
