package com.stockpro.alert.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "alert.rabbitmq")
public class AlertRabbitMqProperties {

    private String exchange;
    private String dlx;
    private String deadQueue;
    private String deadRoutingKey;
    private final Queues queues = new Queues();
    private final RoutingKeys routingKeys = new RoutingKeys();
    private final Retry retry = new Retry();

    @Getter
    @Setter
    public static class Queues {

        private String lowStock;
        private String overstock;
        private String poPending;
        private String overdueReceipt;
        private String system;
        private String email;
    }

    @Getter
    @Setter
    public static class RoutingKeys {

        private String lowStock;
        private String overstock;
        private String poPending;
        private String overdueReceipt;
        private String system;
        private String email;
    }

    @Getter
    @Setter
    public static class Retry {

        private int maxAttempts;
        private long initialInterval;
        private double multiplier;
        private long maxInterval;
    }
}
