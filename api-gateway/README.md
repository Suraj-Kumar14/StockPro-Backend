# StockPro API Gateway

Spring Cloud Gateway is the single backend entry point for StockPro clients. The Angular frontend should call this service only, not individual microservice ports.

## Runtime

| Item | Value |
| --- | --- |
| Service name | `API-GATEWAY` |
| Port | `8080` |
| Eureka URL | `http://localhost:8761/eureka` |
| Frontend origins | `http://localhost:4200`, `http://localhost:4201` |

## Actuator

The gateway exposes the operational endpoints needed for local health checks and route inspection:

| Endpoint | Purpose |
| --- | --- |
| `GET http://localhost:8080/actuator/health` | Gateway health |
| `GET http://localhost:8080/actuator/info` | Gateway info |
| `GET http://localhost:8080/actuator/gateway/routes` | Effective Gateway routes |
| `GET http://localhost:8080/actuator/metrics` | Metrics index |

`/actuator/health`, `/actuator/info`, `/actuator/gateway/**`, and `/actuator/metrics/**` are public in the gateway JWT filter. Business routes still require `Authorization: Bearer <token>`.

## Routes

| Route ID | Predicate | Target |
| --- | --- | --- |
| `auth-service-api-v1` | `/api/v1/auth/**` | `lb://AUTH-SERVICE`, rewritten to `/auth/**` |
| `auth-service` | `/auth/**`, `/oauth2/**`, `/login/oauth2/**` | `lb://AUTH-SERVICE` |
| `product-service` | `/api/v1/products/**` | `lb://PRODUCT-SERVICE` |
| `warehouse-service` | `/api/v1/warehouses/**`, `/api/v1/stocks/**` | `lb://WAREHOUSE-SERVICE` |
| `supplier-service` | `/api/v1/suppliers/**` | `lb://SUPPLIER-SERVICE` |
| `purchase-service` | `/api/v1/purchase-orders/**` | `lb://PURCHASE-SERVICE` |
| `movement-service` | `/api/v1/movements/**` | `lb://MOVEMENT-SERVICE` |
| `alert-service` | `/api/v1/alerts/**` | `lb://ALERT-SERVICE` |
| `payment-service` | `/api/v1/payments/**` | `lb://PAYMENT-SERVICE` |
| `report-service` | `/api/v1/reports/**` | `lb://REPORT-SERVICE` |

Legacy routes such as `/products/**`, `/warehouses/**`, `/stock/**`, `/suppliers/**`, `/purchase-orders/**`, `/movements/**`, `/alerts/**`, `/payments/**`, and `/reports/**` are preserved and rewritten to their `/api/v1/**` downstream paths.

## Authentication And OAuth

Public routes include login, registration, password reset, OTP, Google OAuth redirects, Swagger docs, and gateway health/info.

Google OAuth should be started with a browser redirect:

```text
window.location.href = API_GATEWAY_BASE_URL + "/oauth2/authorization/google"
```

Do not call the OAuth authorization URL with Angular `HttpClient`.

## Startup Order

1. Start MySQL, RabbitMQ, and Redis if the downstream services need them.
2. Start `eureka-server`.
3. Start `auth-service` and the business services.
4. Start `api-gateway`.
5. Start the Angular frontend with API base URL `http://localhost:8080/api/v1`.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `/actuator/health` returns 404 | Confirm the gateway was restarted from the current Maven classpath. If Swagger works on `:8080` but actuator returns `404`, stop the stale IDE process, reimport Maven, and restart `api-gateway`. |
| `/actuator/gateway/routes` is empty | Confirm routes are loaded from `application.yml` and Eureka is reachable. |
| Gateway returns 503 | Confirm the target service is registered in Eureka with the expected service name. |
| Gateway returns 401 | Confirm the route is not public and the request includes `Authorization: Bearer <token>`. |
| Browser CORS failure | Confirm frontend origin is `http://localhost:4200` or `http://localhost:4201`. |
