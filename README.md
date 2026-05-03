# StockPro Inventory Management System

StockPro is a Spring Boot microservices inventory platform with an Angular client, API Gateway, Eureka service discovery, role-based security, and service-level modules for product, warehouse, supplier, purchase, movement, alert, payment, and reporting workflows.

## Case Study

StockPro models the backend for a growing inventory operation that needs centralized authentication, product catalog control, warehouse stock visibility, supplier and purchase management, stock movement audit trails, operational alerts, payment tracking, and management reports. The API Gateway is the only public backend entry point for the frontend.

## Roles

| Role | Typical responsibilities |
| --- | --- |
| `ADMIN` | User administration, global dashboards, service-level oversight |
| `INVENTORY_MANAGER` | Product, stock, warehouse, movement, and report management |
| `WAREHOUSE_STAFF` | Warehouse operations, stock receipt, issue, transfer, and audit |
| `PURCHASE_OFFICER` | Supplier, purchase order, goods receipt, and payment workflows |

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.2.x |
| Gateway | Spring Cloud Gateway, Spring Cloud LoadBalancer |
| Discovery | Netflix Eureka |
| Security | JWT, Spring Security in services, Google OAuth in `auth-service` |
| Persistence | MySQL |
| Messaging | RabbitMQ where configured |
| Cache | Redis where configured |
| Observability | Spring Boot Actuator, Spring Boot Admin |
| API docs | Springdoc OpenAPI / Swagger UI |
| Frontend | Angular |

## Architecture

```text
Angular frontend
  |
  | http://localhost:8080 only
  v
API Gateway (8080)
  |
  | lb:// service discovery through Eureka (8761)
  v
Auth, Product, Warehouse, Supplier, Purchase, Movement, Alert, Payment, Report services
  |
  v
MySQL, RabbitMQ, Redis as required by each service
```

## Services And Ports

| Service | Eureka name | Default port | Purpose |
| --- | --- | --- | --- |
| `api-gateway` | `API-GATEWAY` | `8080` | Single public API entry point |
| `auth-service` | `AUTH-SERVICE` | `8081` | Login, registration, JWT, OTP, Google OAuth, users |
| `product-service` | `PRODUCT-SERVICE` | `8082` | Product catalog and product summaries |
| `warehouse-service` | `WAREHOUSE-SERVICE` | `8083` | Warehouses, stock levels, stock operations |
| `purchase-service` | `PURCHASE-SERVICE` | `8084` | Purchase orders and receiving workflow |
| `supplier-service` | `SUPPLIER-SERVICE` | `8085` | Supplier records and supplier performance |
| `movement-service` | `MOVEMENT-SERVICE` | `8086` | Stock movement history and analytics |
| `alert-service` | `ALERT-SERVICE` | `8087` | Stock, supplier, purchase, movement, and system alerts |
| `report-service` | `REPORT-SERVICE` | `8088` | Inventory, purchase, payment, alert, and dashboard reports |
| `payment-service` | `PAYMENT-SERVICE` | `8089` | Supplier purchase payment lifecycle |
| `eureka-server` | `EUREKA-SERVER` | `8761` | Service registry |
| `Admin-server` | `admin-server` | `9090` | Spring Boot Admin dashboard |

## Frontend Gateway Rule

The Angular application must call the API Gateway only:

```text
API_GATEWAY_BASE_URL=http://localhost:8080
API_BASE_URL=http://localhost:8080/api/v1
```

Correct examples:

```text
POST http://localhost:8080/api/v1/auth/login
GET  http://localhost:8080/api/v1/products
GET  http://localhost:8080/api/v1/warehouses
GET  http://localhost:8080/api/v1/purchase-orders
```

Do not call service ports such as `8081`, `8082`, `8083`, `8084`, `8085`, `8086`, `8087`, `8088`, or `8089` from the frontend.

## Prerequisites

- Java 17
- Maven 3.9+ or each service Maven wrapper
- MySQL 8
- RabbitMQ if running event-driven workflows
- Redis if running services that use cache
- Node.js and Angular CLI for the frontend

## Database Setup

Each service uses its own database by default:

| Service | Database |
| --- | --- |
| `auth-service` | `auth_db` |
| `product-service` | `product_db` |
| `warehouse-service` | `warehouse_db` |
| `supplier-service` | `supplier_db` |
| `purchase-service` | `purchase_db` |
| `movement-service` | `movement_db` |
| `alert-service` | `alert_db` |
| `payment-service` | `payment_db` |
| `report-service` | `report_db` |

Local configs use `createDatabaseIfNotExist=true` for MySQL URLs. Prefer environment variables for credentials in shared environments.

## Environment Variables

Use environment variables for secrets and environment-specific URLs. Common variables include:

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Shared JWT signing secret for gateway and services |
| `SPRING_DATASOURCE_URL` | Service database URL when supported |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | RabbitMQ connection |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | RabbitMQ credentials |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth credentials for `auth-service` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials where mail is enabled |
| `FRONTEND_URL` | OAuth/login redirect frontend URL |
| `EUREKA_SERVER_URL` | Eureka URL for services that support this variable |

Do not commit real passwords, tokens, or OAuth secrets.

## Startup Order

1. Start MySQL.
2. Start RabbitMQ and Redis if the selected services require them.
3. Start `eureka-server`.
4. Start `Admin-server` if you want service monitoring.
5. Start `auth-service`.
6. Start business services: product, warehouse, supplier, purchase, movement, alert, payment, report.
7. Start `api-gateway`.
8. Start the Angular frontend.

## Run Backend Services

From each service directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Or, if Maven is installed globally:

```powershell
mvn spring-boot:run
```

Example:

```powershell
cd eureka-server
.\mvnw.cmd spring-boot:run

cd ..\api-gateway
.\mvnw.cmd spring-boot:run
```

The repository already contains a `docker-compose.yml` for local infrastructure and selected services. Extend it only when the project intentionally adopts containerized startup for additional services.

## Run Frontend

In the Angular project directory:

```powershell
npm install
npm start
```

Set the frontend API base URL to:

```text
http://localhost:8080/api/v1
```

If the Angular client lives in a separate repository or local workspace, keep its environment or app-config base URL pointed at the gateway and not at individual microservice ports.

For Google OAuth, redirect the browser to:

```text
http://localhost:8080/oauth2/authorization/google
```



## Important API Endpoints

All frontend calls should use `http://localhost:8080`.

| Area | Gateway endpoint |


## Role-Based Dashboards

Suggested frontend dashboard routing:

| Role | Dashboard focus |
| --- | --- |
| `ADMIN` | Users, service health, executive reports |
| `INVENTORY_MANAGER` | Products, warehouses, stock health, movements, reports |
| `WAREHOUSE_STAFF` | Receive, issue, transfer, reserve, release, and audit stock |
| `PURCHASE_OFFICER` | Suppliers, purchase orders, receipts, payments |




## Git Workflow

1. Create a feature branch from the current working branch.
2. Keep changes scoped to one service or feature where possible.
3. Run affected service tests before pushing.
4. Do not commit secrets or local machine credentials.
5. Open a pull request with changed services, test evidence, and migration/config notes.


```text
InventoryManagementSystemApp/
  api-gateway/
  auth-service/
  product-service/
  warehouse-service/
  supplier-service/
  purchase-service/
  movement-service/
  alert-service/
  payment-service/
  report-service/
  eureka-server/
  Admin-server/
  docker-compose.yml
  README.md
```

## Future Improvements

- Move all local fallback secrets to developer-only profiles.
- Add a complete compose profile for all services once container startup is standardized.
- Add contract tests for gateway routes and frontend API base URLs.
- Centralize service configuration with Spring Cloud Config if the system grows.
- Add gateway rate limiting and request tracing after baseline functionality is stable.
- Add CI checks for service tests, route config validation, and secret scanning.

## Service Docs

- [API Gateway](./api-gateway/README.md)
- [Auth Service](./auth-service/README.md)
- [Product Service](./product-service/README.md)
- [Payment Service](./payment-service/README.md)
- [Eureka Server](./eureka-server/README.md)
- [Admin Server](./Admin-server/README.md)
