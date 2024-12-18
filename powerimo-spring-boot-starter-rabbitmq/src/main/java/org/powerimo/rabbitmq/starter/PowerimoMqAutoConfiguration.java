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
@EnableConfigurationProperties(PowerimoMqSpringParameters.class)
@ConditionalOnProperty(value = "powerimo.mq.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class PowerimoMqAutoConfiguration {
    public static final String CREATED = "CREATED";
    public static final String STARTED = "STARTED";
    private final PowerimoMqSpringParameters parameters;
    private ObjectMapper objectMapper;
    private static final int LOG_LINE_LENGTH = 80;

    public PowerimoMqAutoConfiguration(PowerimoMqSpringParameters mqParameters) {
        this.parameters = mqParameters;

        if (mqParameters.getShowParametersOnStartup()) {
            log.info(Strings.repeat("=", LOG_LINE_LENGTH));
            log.info("@   Powerimo MQ parameters");
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
    @ConditionalOnMissingBean(MQListener.class)
    public MQListener mqListener(MQParameters mqParameters, MQMessageHandler mqMessageHandler) {
        var listener = new RabbitListener(mqParameters, mqMessageHandler);
        log.debug(formatValue("@ bean MQListener", CREATED));
        if (parameters.isAutoStart()) {
            listener.start();
            log.debug(formatValue("@ bean MQListener start", STARTED));
        } else {
            log.info(formatValue("@ bean MQListener start", "DISABLED"));
        }
        return listener;
    }

    @Bean
    @ConditionalOnMissingBean(MQPublisher.class)
    public MQPublisher mqPublisher(MQParameters mqParameters, MQPayloadConverter payloadConverter) {
        var bean = new MQStandardPublisher(mqParameters, payloadConverter);
        log.debug(formatValue("@ bean MQPublisher", CREATED));
        return bean;
    }

    @Bean
    @ConditionalOnMissingBean(MQPayloadConverter.class)
    public MQPayloadConverter mqPayloadConverter(ObjectMapper objectMapper1) {
        var bean = new StandardPayloadConverter(objectMapper1);
        log.debug(formatValue("@ bean MQPayloadConverter", CREATED));
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
    @ConditionalOnMissingBean(MQMessageHandler.class)
    public MQMessageHandler mqMessageHandler() {
        var handler = new MQStandardMessageHandler();
        log.debug(formatValue("@ bean MQMessageHandler", CREATED));
        return handler;
    }

}
