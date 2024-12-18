package org.powerimo.rabbitmq.starter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.powerimo.common.utils.Utils;
import org.powerimo.rabbitmq.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(RabbitParameters.class)
@ConditionalOnProperty(value = "powerimo.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RabbitAutoConfiguration {
    public static final String CREATED = "CREATED";
    public static final String STARTED = "STARTED";
    private final RabbitParameters parameters;
    private ObjectMapper objectMapper;
    private static final int LOG_LINE_LENGTH = 80;

    public RabbitAutoConfiguration(RabbitParameters mqParameters) {
        this.parameters = mqParameters;

        if (mqParameters.getShowParametersOnStartup()) {
            log.info(Strings.repeat("=", LOG_LINE_LENGTH));
            log.info("@   RabbitMQ parameters");
            log.info(Strings.repeat("=", LOG_LINE_LENGTH));
            log.info(formatValue("Enabled", mqParameters.getEnabled()));
            log.info(formatValue("Application ID (senderId)", mqParameters.getAppId()));
            log.info(formatValue("URL", mqParameters.getUrl()));
            log.info(formatValue("User", mqParameters.getUser()));
            log.info(formatValue("Queue", mqParameters.getUser()));
            log.info(formatValue("Exchange tasks", mqParameters.getExchangeTasks()));
            log.info(formatValue("Exchange events", mqParameters.getExchangeEvents()));
            log.info(formatValue("Auto start", parameters.isAutoStart()));
        }
    }

    public String formatValue(String name, Object value) {
        return Utils.formatLogValue(name, value);
    }

    @Bean
    @ConditionalOnMissingBean(RabbitQueueListener.class)
    public RabbitQueueListener rabbitQueueListener(org.powerimo.rabbitmq.RabbitParameters rabbitParameters, RabbitMessageHandler rabbitMessageHandler) {
        var listener = new StandardRabbitQueueListener(rabbitParameters, rabbitMessageHandler);
        log.debug(formatValue("@ bean RabbitQueueListener", CREATED));
        if (parameters.isAutoStart()) {
            listener.start();
            log.debug(formatValue("@ bean RabbitQueueListener start", STARTED));
        } else {
            log.info(formatValue("@ bean RabbitQueueListener start", "DISABLED"));
        }
        return listener;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitMessagePublisher.class)
    public RabbitMessagePublisher rabbitMessagePublisher(org.powerimo.rabbitmq.RabbitParameters rabbitParameters, RabbitPayloadConverter rabbitPayloadConverter) {
        var bean = new StandardRabbitMessagePublisher(rabbitParameters, rabbitPayloadConverter);
        log.debug(formatValue("@ bean RabbitMessagePublisher", CREATED));
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitPayloadConverter.class)
    public RabbitPayloadConverter rabbitPayloadConverter(ObjectMapper objectMapper1) {
        var bean = new StandardRabbitPayloadConverter(objectMapper1);
        log.debug(formatValue("@ bean RabbitPayloadConverter", CREATED));
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper(){
        if (this.objectMapper != null)
            return objectMapper;
        this.objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        log.debug(formatValue("@ bean ObjectMapper", CREATED));
        return objectMapper;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitMessageHandler.class)
    public RabbitMessageHandler rabbitMessageHandler() {
        var handler = new StandardRabbitMessageHandler();
        log.debug(formatValue("@ bean RabbitMessageHandler", CREATED));
        return handler;
    }

}
