package org.powerimo.rabbitmq.starter;

import org.junit.jupiter.api.Test;
import org.powerimo.rabbitmq.MQListener;
import org.powerimo.rabbitmq.MQPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {PowerimoMqAutoConfiguration.class})
public class TestPowerimoMqAutoConfiguration {
    @Autowired
    private AnnotationConfigApplicationContext context;

    @Test
    void testAutoConfig() {
        var bean = context.getBean(PowerimoMqAutoConfiguration.class);
        assertNotNull(bean);
    }

    @Test
    void testListener() {
        var bean = context.getBean(MQListener.class);
        assertNotNull(bean);
    }

    @Test
    void testHandler() {
        var bean = context.getBean(MQListener.class);
        assertNotNull(bean);
    }

    @Test
    void testPublisher() {
        var bean = context.getBean(MQPublisher.class);
        assertNotNull(bean);
    }
}
