# API Gateway

`api-gateway` is the single entry point for frontend clients.

## Purpose

This service:

- receives requests from frontend applications
- validates JWT on protected routes
- forwards requests to downstream services
- applies CORS configuration

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- Spring Cloud Gateway
- Eureka Client
- JWT (`jjwt`)
- Spring Actuator

## Default Configuration

| Item | Value |
| --- | --- |
| Application Name | `api-gateway` |
| Port | `8080` |

## Authentication Filter Behavior

Protected routes use the custom `AuthenticationFilter`.

It checks:

1. `Authorization` header exists
2. header starts with `Bearer `
3. JWT is valid
4. JWT is not expired

If valid, the gateway forwards the request and adds:

- `X-User-Email`

If invalid, it returns a JSON `401 Unauthorized` response.

## Current Route Configuration

| Route Id | Incoming Path | Target |
| --- | --- | --- |
| `auth-service` | `/auth/**` | `lb://AUTH-SERVICE` |
| `product-service` | `/api/v1/products/**` | `${PRODUCT_SERVICE_URL:http://localhost:8082}` |
| `warehouse-service` | `/api/v1/warehouses/**`, `/api/v1/stock/**` | `${WAREHOUSE_SERVICE_URL:http://localhost:8083}` |
| `purchase-service` | `/api/v1/purchase-orders/**` | `${PURCHASE_SERVICE_URL:http://localhost:8084}` |
| `supplier-service` | `/api/v1/suppliers/**` | `${SUPPLIER_SERVICE_URL:http://localhost:8085}` |
| `movement-service` | `/api/v1/movements/**` | `${MOVEMENT_SERVICE_URL:http://localhost:8086}` |
| `alert-service` | `/api/v1/alerts/**` | `${ALERT_SERVICE_URL:http://localhost:8087}` |
| `report-service` | `/api/v1/reports/**` | `${REPORT_SERVICE_URL:http://localhost:8088}` |

## CORS

Current CORS config allows:

- origin: `http://localhost:4200`
- methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

## Important Configuration Properties

```yaml
server:
  port: ${PORT:8080}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}

app:
  jwt:
    secret: ${JWT_SECRET:...}
```

## How It Connects With Other Services

- validates JWT created by `auth-service`
- routes `/auth/**` to `auth-service`
- routes product and future business-module traffic to downstream services
- registers with `eureka-server`

## Current Integration Note

The current product route is not yet aligned with the current `product-service` code:

- gateway path: `/api/v1/products/**`
- product-service path: `/products/**`
- gateway default product port: `8082`
- product-service default port: `8083`

Frontend calls through the gateway will need that alignment before product APIs work end to end.

## Run

```bash
mvnw.cmd spring-boot:run
```
