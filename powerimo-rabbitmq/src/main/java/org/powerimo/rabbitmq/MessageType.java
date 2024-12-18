package org.powerimo.rabbitmq;

public enum MessageType {
    UNKNOWN,
    TASK,
    TASK_RESULT,
    EVENT,
    ERROR;
}
