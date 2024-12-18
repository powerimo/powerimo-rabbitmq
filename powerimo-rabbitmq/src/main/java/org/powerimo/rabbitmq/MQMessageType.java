package org.powerimo.rabbitmq;

public enum MQMessageType {
    UNKNOWN,
    TASK,
    TASK_RESULT,
    EVENT,
    ERROR;
}
