package org.powerimo.rabbitmq;

import lombok.Data;

@Data
public class LocalParameters implements RabbitParameters {
    private boolean isEnabled = true;
    private String url = "amqp://localhost";
    private String user;
    private String password;
    private String host;
    private String virtualHost;
    private Integer port;
    private String exchangeTasks = "tasks";
    private String exchangeEvents = "events";
    private String queue;
    private String senderId = "default-app-id";
    private boolean showParametersOnStartup = true;

    @Override
    public boolean getEnabled() {
        return false;
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
        return senderId;
    }

    @Override
    public boolean getShowParametersOnStartup() {
        return showParametersOnStartup;
    }
}
