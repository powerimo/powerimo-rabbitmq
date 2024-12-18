package org.powerimo.rabbitmq;

public interface RabbitParameters {
    boolean getEnabled();
    String getUrl();
    String getUser();
    String getPassword();
    String getHost();
    String getVirtualHost();
    Integer getPort();
    String getTasksExchange();
    String getEventsExchange();
    String getQueue();
    String getSenderId();
    boolean getShowParametersOnStartup();
}
