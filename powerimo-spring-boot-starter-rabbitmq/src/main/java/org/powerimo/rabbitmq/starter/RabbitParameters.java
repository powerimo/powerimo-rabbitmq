package org.powerimo.rabbitmq.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "powerimo.rabbitmq")
@Data
public class RabbitParameters implements org.powerimo.rabbitmq.RabbitParameters {
    private String url = "amqp://localhost:5672";
    private boolean enabled = true;
    private String host;
    private String virtualHost;
    private int port = 5672;
    private String user;
    private String password;
    private boolean useDefaultUnsupportedHandlers = true;
    private String queue;
    private boolean autoStart = true;
    private String appId = "default-app-id";
    private String exchangeTasks = "tasks";
    private String exchangeEvents = "events";
    private String namespacePrefix;
    private boolean showParametersOnStartup = true;

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getVirtualHost() {
        return virtualHost;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getTasksExchange() {
        return exchangeTasks;
    }

    @Override
    public String getEventsExchange() {
        return exchangeEvents;
    }

    @Override
    public String getQueue() {
        return queue;
    }

    @Override
    public String getSenderId() {
        return appId;
    }

    @Override
    public boolean getShowParametersOnStartup() {
        return showParametersOnStartup;
    }
}
