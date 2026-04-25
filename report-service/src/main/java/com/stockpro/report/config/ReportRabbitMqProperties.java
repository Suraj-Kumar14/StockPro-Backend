package com.stockpro.report.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "report.rabbitmq")
public class ReportRabbitMqProperties {

    @NotBlank
    private String exchange = "stockpro.report.exchange";

    @Valid
    private Queues queues = new Queues();

    @Valid
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {

        @NotBlank
        private String snapshotCompleted = "report.snapshot.completed.queue";

        @NotBlank
        private String generationRequested = "report.generation.requested.queue";

        @NotBlank
        private String generationCompleted = "report.generation.completed.queue";
    }

    @Getter
    @Setter
    public static class RoutingKeys {

        @NotBlank
        private String snapshotCompleted = "report.snapshot.completed";

        @NotBlank
        private String generationRequested = "report.generation.requested";

        @NotBlank
        private String generationCompleted = "report.generation.completed";
    }
}
