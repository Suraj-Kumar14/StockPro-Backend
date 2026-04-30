# Payment Service

`payment-service` handles Razorpay order creation, signature verification, and payment history lookup for StockPro.

## Defaults

- Service name: `PAYMENT-SERVICE`
- Local port: `8090`
- Gateway path: `/payments/**`
- Swagger UI: `http://localhost:8090/swagger-ui.html`

## Main Endpoints

- `POST /payments/create-order`
- `POST /payments/verify`
- `GET /payments/{id}`
- `GET /payments/order/{orderId}`
- `GET /payments/purchase-order/{poId}`
- `GET /payments/user/{userId}`
- `GET /payments/status/{status}`

## Run

```bash
mvnw.cmd spring-boot:run
```

## Test

```bash
mvnw.cmd test
```

## Notes

- The service registers with Eureka using the name `PAYMENT-SERVICE`.
- `api-gateway` is already configured to route `/payments/**` to this service.
- The frontend currently defines payment DTOs and endpoint constants, but no purchase-order payment flow is wired to the UI yet.
