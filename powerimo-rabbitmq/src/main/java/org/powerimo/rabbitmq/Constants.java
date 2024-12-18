package org.powerimo.rabbitmq;

public class Constants {
    public static final String EVENT_PING = "PING";
    public static final String EVENT_PONG = "PONG";
    public static final String TYPE_MESSAGE_TASK = "task";
    public static final String TYPE_MESSAGE_TASK_RESULT = "task_result";
    public static final String TYPE_MESSAGE_EVENT = "event";
    public static final String TYPE_MESSAGE_ERROR = "error";
    public static final String HEADER_NAME = "name";
    public static final String HEADER_RESULT_CODE = "result_code";
    public static final String HEADER_RESULT_MESSAGE = "result_message";
    public static final String HEADER_PROTOCOL_VERSION = "protocol_version";
    public static final String HEADER_PAYLOAD_CLASS = "payload_class";
    public static final String HEADER_MESSAGE_TYPE = "message_type";
    public static final String PROTOCOL_VERSION_1_0 = "1.0";
    public static final String PROTOCOL_VERSION_1_1 = "1.1";
    public static final String PROTOCOL_VERSION_DEFAULT = PROTOCOL_VERSION_1_1;
    public static final int DEFAULT_MQ_PORT = 5672;
    public static final String CONTENT_TYPE_JSON = "application\\json";
}
