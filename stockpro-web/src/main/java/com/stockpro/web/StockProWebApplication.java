package com.stockpro.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class StockProWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockProWebApplication.class, args);
    }
}
