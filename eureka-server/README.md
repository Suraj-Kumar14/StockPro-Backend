# Eureka Server

`eureka-server` is the discovery registry for the StockPro microservices.

## Purpose

This service allows backend services to:

- register themselves
- discover other services by name
- view registered instances from the Eureka dashboard

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- Spring Cloud Netflix Eureka Server
- Spring Actuator

## Default Configuration

| Item | Value |
| --- | --- |
| Port | `8761` |
| Dashboard | `http://localhost:8761` |

## Main Behavior

- runs as a standalone service registry
- does not register itself with Eureka
- does not fetch registry data from another Eureka instance

## Important Configuration

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

## How It Connects With Other Services

- `auth-service` registers here
- `product-service` registers here
- `api-gateway` registers here
- `Admin-server` registers here

Other StockPro services should also register here when added.

## Run

```bash
mvnw.cmd spring-boot:run
```
