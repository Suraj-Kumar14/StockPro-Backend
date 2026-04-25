package com.stockpro.purchase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "alert.rabbitmq")
public class AlertRabbitMqProperties {

    private String exchange;
    private final RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class RoutingKeys {

        private String poPending;
        private String overdueReceipt;
    }
}
