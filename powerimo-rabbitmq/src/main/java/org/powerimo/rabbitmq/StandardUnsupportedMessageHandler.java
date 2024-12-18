package org.powerimo.rabbitmq;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StandardUnsupportedMessageHandler implements CommandHandler {

    @Override
    public void handleMessage(Message message) {
        log.info("Unsupported message: {}", message);
    }
}
