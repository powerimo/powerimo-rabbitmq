package org.powerimo.rabbitmq;

public class PowerimoMqException extends RuntimeException {
    public PowerimoMqException() {
        super();
    }

    public PowerimoMqException(String message) {
        super(message);
    }

    public PowerimoMqException(String message, Throwable cause) {
        super(message, cause);
    }

    public PowerimoMqException(Throwable cause) {
        super(cause);
    }
}
