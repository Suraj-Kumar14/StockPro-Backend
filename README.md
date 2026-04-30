# StockPro Inventory Management System

This repository currently contains the core backend services built so far for the StockPro platform.

## Services

| Service | Default Port | Purpose |
| --- | --- | --- |
| `eureka-server` | `8761` | Service registry for discovery |
| `Admin-server` | `9090` | Spring Boot Admin monitoring dashboard |
| `auth-service` | `8081` | User registration, login, JWT, OTP, roles |
| `product-service` | `8083` | Product master catalogue, barcode lookup, low-stock query |
| `payment-service` | `8090` | Razorpay order creation, payment verification, payment history |
| `api-gateway` | `8080` | Single entry point for frontend clients |

## How The Services Connect

1. `eureka-server` starts first and acts as the discovery registry.
2. `auth-service`, `product-service`, `api-gateway`, and `Admin-server` register with Eureka.
3. `auth-service` handles registration, login, OTP verification, and issues JWT tokens.
4. `api-gateway` validates JWT on protected routes and forwards requests to downstream services.
5. `product-service` validates JWT again for method-level security and manages product master data.
6. `payment-service` registers with Eureka and is exposed through the gateway under `/payments/**`.
7. `product-service` is designed to call `warehouse-service` for live stock quantity when serving `getLowStockProducts()`.
8. `Admin-server` monitors services that expose actuator endpoints and are configured as admin clients.

## Current Backend Scope

Implemented services in this repository:

- `auth-service`
- `product-service`
- `payment-service`
- `api-gateway`
- `eureka-server`
- `Admin-server`

Referenced but not yet present here as standalone modules:

- `warehouse-service`
- `purchase-service`
- `supplier-service`
- `movement-service`
- `alert-service`
- `report-service`

## Recommended Startup Order

1. `eureka-server`
2. `Admin-server`
3. `auth-service`
4. `product-service`
5. `payment-service`
6. `api-gateway`

## Local Run Commands

From each service folder, run:

```bash
mvnw.cmd spring-boot:run
```

If you are not using the Maven wrapper:

```bash
mvn spring-boot:run
```

## Useful URLs

| Item | URL |
| --- | --- |
| Eureka Dashboard | `http://localhost:8761` |
| Admin Server | `http://localhost:9090` |
| API Gateway | `http://localhost:8080` |
| Auth Swagger | `http://localhost:8081/swagger-ui/index.html` |
| Product Swagger | `http://localhost:8083/swagger-ui/index.html` |
| Payment Swagger | `http://localhost:8090/swagger-ui.html` |

## Frontend Integration

For frontend work, use the API Gateway as the public base URL:

```text
http://localhost:8080
```

Authentication flow:

1. Frontend calls `POST /auth/user/login`
2. `auth-service` returns a JWT
3. Frontend stores the token
4. Frontend sends `Authorization: Bearer <token>` on protected requests
5. `api-gateway` validates the JWT and forwards the request
6. Downstream service applies its own authorization rules

## Role Model In Use

Current role values issued by `auth-service`:

- `ADMIN`
- `INVENTORY_MANAGER`
- `WAREHOUSE_STAFF`
- `PURCHASE_OFFICER`

Important note:

- `product-service` currently accepts manager-level writes using `MANAGER` at controller level, but its JWT filter also maps `INVENTORY_MANAGER` to `ROLE_MANAGER` for compatibility with the existing `auth-service` token format.

## Important Integration Notes

There are a few current alignment points in the codebase that should be kept in mind:

1. `product-service` exposes endpoints under `/products/**`.
2. `payment-service` exposes endpoints under `/payments/**`, and `api-gateway` is already configured to route those requests through Eureka.
3. `product-service` low-stock logic expects a warehouse endpoint for live quantities, but `warehouse-service` is not part of this repository yet.
4. `auth-service` includes Spring Boot Admin client support, but the other services are not fully aligned yet for uniform admin monitoring.

If you want fully working end-to-end frontend integration through the gateway, align the gateway product route and target port with the current `product-service`.

## Service Docs

- [Auth Service](./auth-service/README.md)
- [Product Service](./product-service/README.md)
- [Payment Service](./payment-service/README.md)
- [API Gateway](./api-gateway/README.md)
- [Eureka Server](./eureka-server/README.md)
- [Admin Server](./Admin-server/README.md)
