# Admin Server

`Admin-server` provides the monitoring dashboard for StockPro backend services.

## Purpose

This service is used to:

- view registered Spring Boot applications
- inspect actuator health
- inspect metrics and environment information
- monitor service availability during development

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- Spring Boot Admin Server
- Eureka Client

## Default Configuration

| Item | Value |
| --- | --- |
| Application Name | `admin-server` |
| Port | `9090` |
| Dashboard | `http://localhost:9090` |

## Important Configuration

```yaml
server:
  port: ${ADMIN_SERVER_PORT:9090}

spring:
  application:
    name: admin-server

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka}
```

## How It Connects With Other Services

- registers with `eureka-server`
- monitors services that expose actuator endpoints
- shows services that are configured as Spring Boot Admin clients

## Current Monitoring Scope

At the moment, `auth-service` already includes Spring Boot Admin client support.

Other services may need extra client dependencies or configuration if you want them to appear consistently in the admin dashboard.

## Run

```bash
mvnw.cmd spring-boot:run
```
