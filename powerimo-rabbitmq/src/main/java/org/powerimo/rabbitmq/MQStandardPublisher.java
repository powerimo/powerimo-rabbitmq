package org.powerimo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Setter
@Getter
public class MQStandardPublisher implements MQPublisher {
    private MQPayloadConverter payloadConverter;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private final String CONTENT_TYPE_JSON = "application\\json";
    private final MQParameters mqParameters;

    public MQStandardPublisher(MQParameters mqParameters, MQPayloadConverter payloadConverter1) {
        this.mqParameters = mqParameters;
        this.payloadConverter = payloadConverter1;
    }

    @Override
    public MQMessage sendMessage(@NonNull MQMessage message, @NonNull String exchangeName, String routingKey) {
        try {
            if (connectionFactory == null) {
                log.trace("Connection factory is not initialized. Going to initialization.");
                initConnection();
            }
            channel = connection.createChannel();
            var rkey = routingKey != null ? routingKey : "";
            try {
                var properties = MQUtils.prepareProperties(message, mqParameters.getSenderId(), CONTENT_TYPE_JSON);
                channel.basicPublish(exchangeName, rkey, properties, prepareBody(message));
                log.debug("[->MQ] message is sent: {} to {}, routingKey={}", message, exchangeName, rkey);
            } finally {
                channel.close();
            }
            return message;
        } catch (Exception ex) {
            throw new PowerimoMqException("Exception on sending MQ message", ex);
        }
    }

    @Override
    public MQMessage sendEvent(String name, Object payload) {
        if (mqParameters.getEventsExchange() == null) {
            throw new PowerimoMqException("Event message cannot be sent: event exchange name is not specified");
        }
        MQMessage message = MQMessage.builder()
                .name(name)
                .payload(payload)
                .payloadClass(MQUtils.extractPayloadClass(payload))
                .senderId(mqParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .typeMessage(MQMessageType.EVENT)
                .build();
        return sendMessage(message, mqParameters.getEventsExchange(), "");
    }

    @Override
    public MQMessage sendTask(String name, Object payload, String routingKey) {
        if (mqParameters.getTasksExchange() == null) {
            throw new PowerimoMqException("Task message cannot be sent: event exchange name is not specified");
        }
        MQMessage message = MQMessage.builder()
                .name(name)
                .payload(payload)
                .payloadClass(MQUtils.extractPayloadClass(payload))
                .senderId(mqParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .typeMessage(MQMessageType.TASK)
                .routingKey(routingKey)
                .build();
        return sendMessage(message, mqParameters.getTasksExchange(), routingKey);
    }

    @Override
    public MQMessage sendTaskResult(String name, Object payload, Integer resultCode, String resultMessage, MQMessage sourceMessage) {
        if (mqParameters.getTasksExchange() == null) {
            throw new PowerimoMqException("TaskResult message cannot be sent: event exchange name is not specified");
        }
        var sourceMessageId = sourceMessage == null ? null : sourceMessage.getMessageId();
        var sourceAppId = sourceMessage == null ? null : sourceMessage.getSenderId();
        MQMessage message = MQMessage.builder()
                .name(name)
                .payload(payload)
                .payloadClass(MQUtils.extractPayloadClass(payload))
                .senderId(mqParameters.getSenderId())
                .contentType(CONTENT_TYPE_JSON)
                .result(resultMessage)
                .resultCode(resultCode)
                .sourceMessageId(sourceMessageId)
                .sourceSenderId(sourceAppId)
                .typeMessage(MQMessageType.TASK_RESULT)
                .routingKey(sourceAppId)
                .build();
        return sendMessage(message, mqParameters.getTasksExchange(), sourceAppId);
    }

    @Override
    public void setConverter(MQPayloadConverter converter) {
        this.payloadConverter = converter;
    }

    private void initConnection() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        if (mqParameters.getUrl() == null && mqParameters.getHost() == null) {
            throw new PowerimoMqException("MQ Connection is not initialized: both URL and host are empty");
        }
        if (connectionFactory == null) {
            connectionFactory = new ConnectionFactory();
            if (mqParameters.getUrl() != null) {
                connectionFactory.setUri(mqParameters.getUrl());
            } else {
                connectionFactory.setHost(mqParameters.getHost());
                connectionFactory.setVirtualHost(mqParameters.getVirtualHost());
                connectionFactory.setPort(mqParameters.getPort());
            }
            if (mqParameters.getUser() != null) {
                connectionFactory.setUsername(mqParameters.getUser());
                connectionFactory.setPassword(mqParameters.getPassword());
            }
        }
        if (connection == null) {
            connection = connectionFactory.newConnection();
        }
    }

    private byte[] prepareBody(MQMessage message) {
        if (payloadConverter == null)
            throw new PowerimoMqException("Payload converter is missing");
        byte[] data = null;
        if (message.getPayload() != null) {
            data = payloadConverter.serializePayload(message.getPayload());
        }
        return data;
    }
}
