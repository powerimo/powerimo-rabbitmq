package org.powerimo.rabbitmq;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MQStandardUnsupportedMessageHandler implements MQCommandHandler {

    @Override
    public void handleMessage(MQMessage message) {
        log.info("Unsupported message: {}", message);
    }
}
