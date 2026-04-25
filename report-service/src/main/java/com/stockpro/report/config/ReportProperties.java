package com.stockpro.report.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "report")
public class ReportProperties {

    @Valid
    private Snapshot snapshot = new Snapshot();

    @Valid
    private DeadStock deadStock = new DeadStock();

    @Valid
    private Export export = new Export();

    @Getter
    @Setter
    public static class Snapshot {

        @NotBlank
        private String cron = "0 0 0 * * *";

        @Min(1)
        private int pageSize = 200;
    }

    @Getter
    @Setter
    public static class DeadStock {

        @Min(1)
        private int thresholdDays = 90;
    }

    @Getter
    @Setter
    public static class Export {

        @NotBlank
        private String directory = "reports";
    }
}
