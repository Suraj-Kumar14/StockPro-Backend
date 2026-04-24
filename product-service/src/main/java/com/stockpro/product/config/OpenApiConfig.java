package com.stockpro.product.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger configuration for Product Service.
 * The bearerAuth scheme lets users test secured APIs from Swagger UI.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StockPro Product Service API")
                        .description("Product master catalogue service used by inventory, warehouse, purchase and alert workflows.")
                        .version("1.0.0")
                        .contact(new Contact().name("StockPro Backend Team")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
