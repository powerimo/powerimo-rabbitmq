package org.powerimo.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class MQUtils {
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

    /**
     * Convert sting to Integer with default value
     * @param s string to convert
     * @param defaultValue default value
     * @return Integer value
     */
    public static Integer stringToIntegerDef(String s, Integer defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static MQMessageType getTypeMessage(MQMessage mqMessage) {
        if (mqMessage.getTypeMessage() != null)
            return mqMessage.getTypeMessage();
        return getTypeMessage(mqMessage.getTypeMessageOriginalString());
    }

    public static MQMessageType getTypeMessage(String typeMessage) {
        if (typeMessage == null) {
            return MQMessageType.UNKNOWN;
        }
        switch (typeMessage.toLowerCase()) {
            case (TYPE_MESSAGE_TASK):
                return MQMessageType.TASK;
            case (TYPE_MESSAGE_TASK_RESULT):
                return MQMessageType.TASK_RESULT;
            case (TYPE_MESSAGE_EVENT):
                return MQMessageType.EVENT;
            case (TYPE_MESSAGE_ERROR):
                return MQMessageType.ERROR;
            default:
                return MQMessageType.UNKNOWN;
        }
    }

    /**
     * Extract message from Rabbit message. Method arguments is the same as com.rabbitmq.client.Consumer.handleDelivery
     * @param s tag, assoiated with consumer
     * @param envelope RabbitMQ envelope
     * @param basicProperties properties of the message
     * @param bytes body
     * @return MQMessage object
     */
    public static MQMessage extractMessage(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        if (basicProperties.getHeaders() == null) {
            throw new PowerimoMqException("Message doesn't contains headers");
        }

        try {
            MQMessage mqMessage = new MQMessage();
            mqMessage.setRoutingKey(envelope.getRoutingKey());
            mqMessage.setContentType(basicProperties.getContentType());
            mqMessage.setName(readProperty(basicProperties, HEADER_NAME));
            var messageTypeString = readProperty(basicProperties, HEADER_MESSAGE_TYPE);
            mqMessage.setTypeMessageOriginalString(messageTypeString);
            mqMessage.setTypeMessage(getTypeMessage(messageTypeString));
            mqMessage.setMessageId(basicProperties.getMessageId());
            mqMessage.setProcessId(basicProperties.getCorrelationId());
            mqMessage.setSenderId(basicProperties.getAppId());
            mqMessage.setResult(readProperty(basicProperties, HEADER_RESULT_MESSAGE));
            mqMessage.setResultCode((Integer) basicProperties.getHeaders().get(HEADER_RESULT_CODE));
            mqMessage.setProtocolVersion(basicProperties.getHeaders().getOrDefault(HEADER_PROTOCOL_VERSION, PROTOCOL_VERSION_DEFAULT).toString());
            mqMessage.setPayload(new String(bytes, StandardCharsets.UTF_8));
            mqMessage.setPayloadClass(readProperty(basicProperties, HEADER_PAYLOAD_CLASS));

            basicProperties.getHeaders().forEach(mqMessage::addParam);

            return mqMessage;
        } catch (Throwable ex) {
            throw new PowerimoMqException("Error reading the message", ex);
        }
    }

    /**
     * Read value of a property
     * @param properties properties from RabbitMQ message
     * @param name the name of property
     * @return string value of the property
     */
    public static String readProperty(AMQP.BasicProperties properties, String name) {
        Object obj = properties.getHeaders().get(name);
        if (obj != null)
            return obj.toString();
        else
            return null;
    }

    public static String extractPayloadClass(Object payload) {
        if (payload == null)
            return null;
        else {
            return payload.getClass().getCanonicalName();
        }
    }

    public String convertToString(byte[] data) {
        if (data == null)
            return null;
        return new String(data, StandardCharsets.UTF_8);
    }

    public static AMQP.BasicProperties prepareProperties(MQMessage message, String appId, String contentType) {
        if (message == null) {
            return new AMQP.BasicProperties.Builder()
                    .appId(appId)
                    .contentType(contentType)
                    .build();
        }

        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
            log.debug("messageID was null. Created a new one: {}", message.getMessageId());
        }
        if (message.getProtocolVersion() == null) {
            message.setProtocolVersion(MQUtils.PROTOCOL_VERSION_DEFAULT);
            log.debug("protocol version was null. Set to default: {}", message.getProtocolVersion());
        }

        var headers = new HashMap<String, Object>();

        if (message.getParams() != null) {
            headers.putAll(message.getParams());
        }

        headers.put(MQUtils.HEADER_MESSAGE_TYPE, message.getTypeMessage().name());
        log.trace("header {} is set to: {}", MQUtils.HEADER_MESSAGE_TYPE, message.getTypeMessage());
        headers.put(MQUtils.HEADER_PROTOCOL_VERSION, message.getProtocolVersion());
        log.trace("header {} is set to: {}", MQUtils.HEADER_PROTOCOL_VERSION, message.getProtocolVersion());
        headers.put(MQUtils.HEADER_NAME, message.getName());
        log.trace("header {} is set to: {}", MQUtils.HEADER_NAME, message.getName());
        headers.put(MQUtils.HEADER_PAYLOAD_CLASS, message.getPayloadClass());
        log.trace("header {} is set to: {}", MQUtils.HEADER_PAYLOAD_CLASS, message.getPayloadClass());
        headers.put(MQUtils.HEADER_RESULT_MESSAGE, message.getResult());
        log.trace("header {} is set to: {}", MQUtils.HEADER_RESULT_MESSAGE, message.getResult());
        headers.put(MQUtils.HEADER_RESULT_CODE, message.getResultCode());
        log.trace("header {} is set to: {}", MQUtils.HEADER_RESULT_CODE, message.getResultCode());

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .appId(appId)
                .messageId(message.getMessageId())
                .correlationId(message.getProcessId())
                .contentType(contentType)
                .headers(headers)
                .build();
        log.trace("message properties is prepared: {}", properties);
        return properties;
    }
}
