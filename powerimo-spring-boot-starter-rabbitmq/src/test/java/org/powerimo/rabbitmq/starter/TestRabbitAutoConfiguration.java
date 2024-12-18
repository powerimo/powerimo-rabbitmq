package org.powerimo.rabbitmq.starter;

import org.junit.jupiter.api.Test;
import org.powerimo.rabbitmq.RabbitMessagePublisher;
import org.powerimo.rabbitmq.RabbitQueueListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {RabbitAutoConfiguration.class})
public class TestRabbitAutoConfiguration {
    @Autowired
    private ApplicationContext context;

    @Test
    void testAutoConfig() {
        var bean = context.getBean(RabbitAutoConfiguration.class);
        assertNotNull(bean);
    }

    @Test
    void testListener() {
        var bean = context.getBean(RabbitQueueListener.class);
        assertNotNull(bean);
    }

    @Test
    void testHandler() {
        var bean = context.getBean(RabbitQueueListener.class);
        assertNotNull(bean);
    }

    @Test
    void testPublisher() {
        var bean = context.getBean(RabbitMessagePublisher.class);
        assertNotNull(bean);
    }
}
