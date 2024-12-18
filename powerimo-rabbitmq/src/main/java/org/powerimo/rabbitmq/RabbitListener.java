package org.powerimo.rabbitmq;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Setter
@Getter
public class RabbitListener implements MQListener {
    private MQMessageHandler messageHandler;
    private Channel mqChannel;
    private ServiceStatus serviceStatus = ServiceStatus.STOPPED;
    private boolean showConnectionInfoOnStartup = true;
    private final MQParameters mqParameters;

    public RabbitListener() {
        this.mqParameters = new MQLocalParameters();
    }

    public RabbitListener(MQParameters mqParameters, MQMessageHandler messageHandler) {
        this.mqParameters = mqParameters;
        this.messageHandler = messageHandler;
    }

    @Override
    public void start() {
        if (serviceStatus == ServiceStatus.RUNNING) {
            log.debug("Service is already started");
            return;
        }
        if (mqParameters.getQueue() == null) {
            throw new PowerimoMqException("Queue is not specified");
        }

        showConnectionInfo();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        try {
            connectionFactory.setUri(getURL());
            if (mqParameters.getUser() != null) {
                connectionFactory.setUsername(mqParameters.getUser());
                connectionFactory.setPassword(mqParameters.getPassword());
            }
            Connection mqConnection = connectionFactory.newConnection();
            mqChannel = mqConnection.createChannel();
            mqChannel.basicConsume(mqParameters.getQueue(), new MQConsumer(mqChannel, messageHandler));
            serviceStatus = ServiceStatus.RUNNING;
            log.info("RabbitListener started on listening queue: {}", this.mqParameters.getQueue());
        } catch (PowerimoMqException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PowerimoMqException("RabbitListener is not started", ex);
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
            throw new PowerimoMqException("RabbitListener stopping exception", ex);
        }
    }

    private String getURL() {
        if (mqParameters.getUrl() != null)
            return mqParameters.getUrl();
        if (mqParameters.getHost() == null) {
            throw new PowerimoMqException("MQ configuration exception: both url and host are empty");
        }
        String result;
        result = "amqp://" + mqParameters.getHost()+ ":" + mqParameters.getPort();
        if (mqParameters.getVirtualHost() != null && !mqParameters.getVirtualHost().isEmpty()) {
            result = result + "/" + mqParameters.getVirtualHost();
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
        if (mqParameters.getUrl() != null)
            log.info("RabbitMQ URL: {}; user: {}", mqParameters.getUrl(), mqParameters.getUser());
        else
            log.info("RabbitMQ host: {}, virtualHost: {}, port: {}, user: {}",
                    mqParameters.getHost(),
                    mqParameters.getVirtualHost(),
                    mqParameters.getPort(),
                    mqParameters.getUser());
        log.info("RabbitMQ queue used: {}", mqParameters.getQueue());
    }

    private static class MQConsumer extends DefaultConsumer {
        private final Channel _channel;
        private final MQMessageHandler _handler;
        public MQConsumer(Channel channel, MQMessageHandler messageHandler) {
            super(channel);
            _channel = channel;
            _handler = messageHandler;
        }

        @Override
        public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
            MQMessage message;
            try {
                message = MQUtils.extractMessage(s, envelope, basicProperties, bytes);
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
                    MQExceptionResolution resolution = _handler.handleException(message, ex1);
                    if (resolution == MQExceptionResolution.REQUEUE) {
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
