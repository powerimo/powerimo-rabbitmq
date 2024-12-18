package org.powerimo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import static org.powerimo.rabbitmq.Constants.CONTENT_TYPE_JSON;

@Slf4j
@Setter
@Getter
public class StandardRabbitMessagePublisher implements RabbitMessagePublisher {
    private RabbitPayloadConverter rabbitPayloadConverter;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private final RabbitParameters rabbitParameters;

    public StandardRabbitMessagePublisher(RabbitParameters rabbitParameters, RabbitPayloadConverter rabbitPayloadConverter1) {
        this.rabbitParameters = rabbitParameters;
        this.rabbitPayloadConverter = rabbitPayloadConverter1;
    }

    @Override
    public Message sendMessage(@NonNull Message message, @NonNull String exchangeName, String routingKey) {
        try {
            if (connectionFactory == null) {
                log.trace("Connection factory is not initialized. Going to initialization.");
                initConnection();
            }
            channel = connection.createChannel();
            var rkey = routingKey != null ? routingKey : "";
            try {
                var properties = RabbitUtils.prepareProperties(message, rabbitParameters.getSenderId(), CONTENT_TYPE_JSON);
                channel.basicPublish(exchangeName, rkey, properties, prepareBody(message));
                log.debug("[->MQ] message is sent: {} to {}, routingKey={}", message, exchangeName, rkey);
            } finally {
                channel.close();
            }
            return message;
        } catch (Exception ex) {
            throw new RabbitException("Exception on sending MQ message", ex);
        }
    }

    @Override
    public Message sendEvent(String name, Object payload) {
        if (rabbitParameters.getEventsExchange() == null) {
            throw new RabbitException("Event message cannot be sent: event exchange name is not specified");
        }
        Message message = Message.builder()
                .name(name)
                .payload(payload)
                .payloadClass(RabbitUtils.extractPayloadClass(payload))
                .senderId(rabbitParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .typeMessage(MessageType.EVENT)
                .build();
        return sendMessage(message, rabbitParameters.getEventsExchange(), "");
    }

    @Override
    public Message sendTask(String name, Object payload, String routingKey) {
        if (rabbitParameters.getTasksExchange() == null) {
            throw new RabbitException("Task message cannot be sent: event exchange name is not specified");
        }
        Message message = Message.builder()
                .name(name)
                .payload(payload)
                .payloadClass(RabbitUtils.extractPayloadClass(payload))
                .senderId(rabbitParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .typeMessage(MessageType.TASK)
                .routingKey(routingKey)
                .build();
        return sendMessage(message, rabbitParameters.getTasksExchange(), routingKey);
    }

    @Override
    public Message sendTaskResult(String name, Object payload, Integer resultCode, String resultMessage, Message sourceMessage) {
        if (rabbitParameters.getTasksExchange() == null) {
            throw new RabbitException("TaskResult message cannot be sent: event exchange name is not specified");
        }
        var sourceMessageId = sourceMessage == null ? null : sourceMessage.getMessageId();
        var sourceAppId = sourceMessage == null ? null : sourceMessage.getSenderId();
        Message message = Message.builder()
                .name(name)
                .payload(payload)
                .payloadClass(RabbitUtils.extractPayloadClass(payload))
                .senderId(rabbitParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .result(resultMessage)
                .resultCode(resultCode)
                .sourceMessageId(sourceMessageId)
                .sourceSenderId(sourceAppId)
                .typeMessage(MessageType.TASK_RESULT)
                .routingKey(sourceAppId)
                .build();
        return sendMessage(message, rabbitParameters.getTasksExchange(), sourceAppId);
    }

    @Override
    public void setConverter(RabbitPayloadConverter converter) {
        this.rabbitPayloadConverter = converter;
    }

    private void initConnection() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        if (rabbitParameters.getUrl() == null && rabbitParameters.getHost() == null) {
            throw new RabbitException("MQ Connection is not initialized: both URL and host are empty");
        }
        if (connectionFactory == null) {
            connectionFactory = new ConnectionFactory();
            if (rabbitParameters.getUrl() != null) {
                connectionFactory.setUri(rabbitParameters.getUrl());
            } else {
                connectionFactory.setHost(rabbitParameters.getHost());
                connectionFactory.setVirtualHost(rabbitParameters.getVirtualHost());
                connectionFactory.setPort(rabbitParameters.getPort());
            }
            if (rabbitParameters.getUser() != null) {
                connectionFactory.setUsername(rabbitParameters.getUser());
                connectionFactory.setPassword(rabbitParameters.getPassword());
            }
        }
        if (connection == null) {
            connection = connectionFactory.newConnection();
        }
    }

    private byte[] prepareBody(Message message) {
        if (rabbitPayloadConverter == null)
            throw new RabbitException("Payload converter is missing");
        byte[] data = null;
        if (message.getPayload() != null) {
            data = rabbitPayloadConverter.serializePayload(message.getPayload());
        }
        return data;
    }
}
