package org.powerimo.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class RabbitUtils {

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

    public static MessageType getTypeMessage(Message message) {
        if (message.getTypeMessage() != null)
            return message.getTypeMessage();
        return getTypeMessage(message.getTypeMessageOriginalString());
    }

    public static MessageType getTypeMessage(String typeMessage) {
        if (typeMessage == null) {
            return MessageType.UNKNOWN;
        }
        switch (typeMessage.toLowerCase()) {
            case (Constants.TYPE_MESSAGE_TASK):
                return MessageType.TASK;
            case (Constants.TYPE_MESSAGE_TASK_RESULT):
                return MessageType.TASK_RESULT;
            case (Constants.TYPE_MESSAGE_EVENT):
                return MessageType.EVENT;
            case (Constants.TYPE_MESSAGE_ERROR):
                return MessageType.ERROR;
            default:
                return MessageType.UNKNOWN;
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
    public static Message extractMessage(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        if (basicProperties.getHeaders() == null) {
            throw new RabbitException("Message doesn't contains headers");
        }

        try {
            Message message = new Message();
            message.setRoutingKey(envelope.getRoutingKey());
            message.setContentType(basicProperties.getContentType());
            message.setName(readProperty(basicProperties, Constants.HEADER_NAME));
            var messageTypeString = readProperty(basicProperties, Constants.HEADER_MESSAGE_TYPE);
            message.setTypeMessageOriginalString(messageTypeString);
            message.setTypeMessage(getTypeMessage(messageTypeString));
            message.setMessageId(basicProperties.getMessageId());
            message.setProcessId(basicProperties.getCorrelationId());
            message.setSenderId(basicProperties.getAppId());
            message.setResult(readProperty(basicProperties, Constants.HEADER_RESULT_MESSAGE));
            message.setResultCode((Integer) basicProperties.getHeaders().get(Constants.HEADER_RESULT_CODE));
            message.setProtocolVersion(basicProperties.getHeaders().getOrDefault(Constants.HEADER_PROTOCOL_VERSION, Constants.PROTOCOL_VERSION_DEFAULT).toString());
            message.setPayload(new String(bytes, StandardCharsets.UTF_8));
            message.setPayloadClass(readProperty(basicProperties, Constants.HEADER_PAYLOAD_CLASS));

            basicProperties.getHeaders().forEach(message::addParam);

            return message;
        } catch (Throwable ex) {
            throw new RabbitException("Error reading the message", ex);
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

    public static AMQP.BasicProperties prepareProperties(Message message, String appId, String contentType) {
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
            message.setProtocolVersion(Constants.PROTOCOL_VERSION_DEFAULT);
            log.debug("protocol version was null. Set to default: {}", message.getProtocolVersion());
        }

        var headers = new HashMap<String, Object>();

        if (message.getParams() != null) {
            headers.putAll(message.getParams());
        }

        headers.put(Constants.HEADER_MESSAGE_TYPE, message.getTypeMessage().name());
        log.trace("header {} is set to: {}", Constants.HEADER_MESSAGE_TYPE, message.getTypeMessage());
        headers.put(Constants.HEADER_PROTOCOL_VERSION, message.getProtocolVersion());
        log.trace("header {} is set to: {}", Constants.HEADER_PROTOCOL_VERSION, message.getProtocolVersion());
        headers.put(Constants.HEADER_NAME, message.getName());
        log.trace("header {} is set to: {}", Constants.HEADER_NAME, message.getName());
        headers.put(Constants.HEADER_PAYLOAD_CLASS, message.getPayloadClass());
        log.trace("header {} is set to: {}", Constants.HEADER_PAYLOAD_CLASS, message.getPayloadClass());
        headers.put(Constants.HEADER_RESULT_MESSAGE, message.getResult());
        log.trace("header {} is set to: {}", Constants.HEADER_RESULT_MESSAGE, message.getResult());
        headers.put(Constants.HEADER_RESULT_CODE, message.getResultCode());
        log.trace("header {} is set to: {}", Constants.HEADER_RESULT_CODE, message.getResultCode());

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
