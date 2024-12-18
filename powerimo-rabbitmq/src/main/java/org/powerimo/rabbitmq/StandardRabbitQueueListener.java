package org.powerimo.rabbitmq;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Setter
@Getter
public class StandardRabbitQueueListener implements RabbitQueueListener {
    private RabbitMessageHandler rabbitMessageHandler;
    private Channel mqChannel;
    private ServiceStatus serviceStatus = ServiceStatus.STOPPED;
    private boolean showConnectionInfoOnStartup = true;
    private final RabbitParameters rabbitParameters;

    public StandardRabbitQueueListener() {
        this.rabbitParameters = new LocalParameters();
    }

    public StandardRabbitQueueListener(RabbitParameters rabbitParameters, RabbitMessageHandler rabbitMessageHandler) {
        this.rabbitParameters = rabbitParameters;
        this.rabbitMessageHandler = rabbitMessageHandler;
    }

    @Override
    public void start() {
        if (serviceStatus == ServiceStatus.RUNNING) {
            log.debug("Service is already started");
            return;
        }
        if (rabbitParameters.getQueue() == null) {
            throw new RabbitException("Queue is not specified");
        }

        showConnectionInfo();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        try {
            connectionFactory.setUri(getURL());
            if (rabbitParameters.getUser() != null) {
                connectionFactory.setUsername(rabbitParameters.getUser());
                connectionFactory.setPassword(rabbitParameters.getPassword());
            }
            Connection mqConnection = connectionFactory.newConnection();
            mqChannel = mqConnection.createChannel();
            mqChannel.basicConsume(rabbitParameters.getQueue(), new MQConsumer(mqChannel, rabbitMessageHandler));
            serviceStatus = ServiceStatus.RUNNING;
            log.info("RabbitListener started on listening queue: {}", this.rabbitParameters.getQueue());
        } catch (RabbitException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RabbitException("RabbitListener is not started", ex);
        }
    }

    @Override
    public void stop() {
        log.debug("Stopping Rabbit listener...");
        try {
            if (serviceStatus == ServiceStatus.STOPPED) {
                log.debug("Service is already stopped. Exit.");
                return;
            }
            if (mqChannel != null && mqChannel.isOpen()) {
                mqChannel.close();
            }
            serviceStatus = ServiceStatus.STOPPED;
        } catch (Exception ex) {
            throw new RabbitException("RabbitListener stopping exception", ex);
        }
    }

    private String getURL() {
        if (rabbitParameters.getUrl() != null)
            return rabbitParameters.getUrl();
        if (rabbitParameters.getHost() == null) {
            throw new RabbitException("MQ configuration exception: both url and host are empty");
        }
        String result;
        result = "amqp://" + rabbitParameters.getHost()+ ":" + rabbitParameters.getPort();
        if (rabbitParameters.getVirtualHost() != null && !rabbitParameters.getVirtualHost().isEmpty()) {
            result = result + "/" + rabbitParameters.getVirtualHost();
        }
        return result;
    }

    @Override
    public ServiceStatus getStatus() {
        return serviceStatus;
    }

    private void showConnectionInfo() {
        if (!showConnectionInfoOnStartup)
            return;
        if (rabbitParameters.getUrl() != null)
            log.info("RabbitMQ URL: {}; user: {}", rabbitParameters.getUrl(), rabbitParameters.getUser());
        else
            log.info("RabbitMQ host: {}, virtualHost: {}, port: {}, user: {}",
                    rabbitParameters.getHost(),
                    rabbitParameters.getVirtualHost(),
                    rabbitParameters.getPort(),
                    rabbitParameters.getUser());
        log.info("RabbitMQ queue used: {}", rabbitParameters.getQueue());
    }

    private static class MQConsumer extends DefaultConsumer {
        private final Channel _channel;
        private final RabbitMessageHandler _handler;
        public MQConsumer(Channel channel, RabbitMessageHandler rabbitMessageHandler) {
            super(channel);
            _channel = channel;
            _handler = rabbitMessageHandler;
        }

        @Override
        public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
            Message message;
            try {
                message = RabbitUtils.extractMessage(s, envelope, basicProperties, bytes);
            } catch (Exception ex) {
                _channel.basicReject(envelope.getDeliveryTag(), false);
                log.error("[MQ] Exception on parsing message. Message was rejected. Source text=({}), Envelope=({}), basicProperties=({}), bytes[]=({})", s, envelope, basicProperties, bytes);
                return;
            }

            try {
                if (_handler != null) {
                    _handler.handleMessage(message);
                    _channel.basicAck(envelope.getDeliveryTag(), false);
                } else {
                    log.warn("The MQ message was successfully delivered but there is no Message handler for processing (please set it by RabbitListener.setMessageHandler). The message will be rejected. ({})", message);
                    _channel.basicReject(envelope.getDeliveryTag(), false);
                }
            } catch (Exception ex1) {
                if (_handler == null) {
                    log.error("Exception on handling message. Message will be rejected and pushed to DLQ. Message={}", message, ex1);
                } else {
                    ExceptionResolution resolution = _handler.handleException(message, ex1);
                    if (resolution == ExceptionResolution.REQUEUE) {
                        log.error("Exception on handling message. Message will rejected with requeue. Message={}", message, ex1);
                    } else {
                        log.error("Exception on handling message. Message will be rejected and pushed to DLQ. Message={}", message, ex1);
                        _channel.basicReject(envelope.getDeliveryTag(), false);
                    }
                }
            }
        }

    }

}
