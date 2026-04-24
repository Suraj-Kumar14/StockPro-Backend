# Product Service

`product-service` manages the StockPro product master catalogue.

## Purpose

This service stores and exposes product master data such as:

- SKU
- product name and description
- category and brand
- barcode
- unit of measure
- cost price and selling price
- reorder configuration
- active/inactive status

It does not own live stock quantity. Live quantity belongs to `warehouse-service`.

## Tech Stack

- Java 17
- Spring Boot 3.2.2
- Spring Data JPA
- Spring Security
- H2
- OpenFeign
- JWT (`jjwt`)
- Swagger / OpenAPI
- Eureka Client

## Default Configuration

| Item | Value |
| --- | --- |
| Application Name | `PRODUCT-SERVICE` |
| Port | `8083` |
| Swagger | `http://localhost:8083/swagger-ui/index.html` |
| H2 Console | `http://localhost:8083/h2-console` |

## Main Features

- create product
- update product
- get product by id
- get product by SKU
- get products by category
- get products by brand
- search product by name
- get all products
- barcode lookup
- deactivate product
- delete product
- low-stock query

## Security Rules

Write operations:

- `MANAGER` only

Read operations:

- `MANAGER`
- `WAREHOUSE_STAFF`
- `PURCHASE_OFFICER`

Compatibility note:

- `auth-service` currently issues `INVENTORY_MANAGER`
- `product-service` maps `INVENTORY_MANAGER` to manager-level access internally

## Product Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/products` | Create product |
| `GET` | `/products/{productId}` | Get by id |
| `GET` | `/products/sku/{sku}` | Get by SKU |
| `GET` | `/products/category/{category}` | Get by category |
| `GET` | `/products/brand/{brand}` | Get by brand |
| `GET` | `/products/barcode/{barcode}` | Get by barcode |
| `GET` | `/products/search?keyword=...` | Search by product name |
| `GET` | `/products/all` | Get all products |
| `GET` | `/products/low-stock` | Get low-stock products |
| `PUT` | `/products/{productId}` | Update product |
| `PUT` | `/products/{productId}/deactivate` | Deactivate product |
| `DELETE` | `/products/{productId}` | Delete product |

## Low Stock Logic

`getLowStockProducts()` compares:

- current quantity from `warehouse-service`
- product `reorderLevel` from this service

This service calls `warehouse-service` through Feign for current stock values.

Current internal client expectation:

- service name: `WAREHOUSE-SERVICE`
- endpoint: `POST /warehouse/internal/stock-levels`

## Important Configuration Properties

```yaml
server:
  port: ${PORT:8083}

app:
  jwt:
    secret: ${JWT_SECRET:...}

warehouse:
  service:
    name: ${WAREHOUSE_SERVICE_NAME:WAREHOUSE-SERVICE}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}
```

## How It Connects With Other Services

- registers with `eureka-server`
- validates JWT issued by `auth-service`
- is intended to be called through `api-gateway`
- calls `warehouse-service` for low-stock calculations

## Current Integration Note

The service exposes `/products/**` on port `8083`, but the current gateway configuration still points product traffic to `/api/v1/products/**` and default port `8082`. Align the gateway before frontend integration.

## Run

```bash
mvnw.cmd spring-boot:run
```

## Test

```bash
mvnw.cmd test
```
