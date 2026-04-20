# 🔐 Auth Service – StockPro

A production-ready **Authentication & Authorization Microservice** built using **Spring Boot**, **JWT**, and **Email OTP verification**.

This service is responsible for:

- User Registration with OTP verification
- Secure Login using JWT
- Forgot Password & Reset Password flow
- Profile management with email verification
- Role-based access foundation

---

## 🚀 Tech Stack

| Technology        | Description                    |
| ----------------- | ------------------------------ |
| Java 17           | Core programming language      |
| Spring Boot 3     | Backend framework              |
| Spring Security   | Authentication & authorization |
| JWT (JJWT)        | Token-based authentication     |
| Spring Data JPA   | Database interaction           |
| H2 Database       | In-memory database (dev)       |
| MySql Database    | For Data Storing               |
| Java Mail Sender  | Email OTP delivery             |
| Lombok            | Boilerplate reduction          |
| Swagger / OpenAPI | API documentation              |

---

## 📁 Project Structure

auth-service
│
├── config # Security, JWT filter, roles
├── controller # REST controllers
├── dtos # Request/Response DTOs
├── entity # JPA entities
├── exception # Custom exceptions & global handler
├── repository # JPA repositories
├── service # Business logic
└── AuthServiceApplication.java

---

## 🔑 Features

### ✅ User Registration (OTP Based)

- User submits registration details
- OTP is sent to email
- Account activated after OTP verification

### ✅ Login

- Email + password authentication
- JWT token generated
- Only active users can login

### ✅ Forgot Password Flow

- OTP sent to email
- OTP verification
- Secure password reset

### ✅ Profile Management

- Update name and phone
- Email change requires OTP verification

### ✅ Role Support (String-based)

Supported roles:

- `ADMIN`
- `INVENTORY_MANAGER`
- `WAREHOUSE_STAFF`
- `PURCHASE_OFFICER`

---

## 🔐 Authentication Flow

1. User logs in → receives JWT token
2. Client sends token in header:

3. JWT Filter validates token
4. Spring Security authenticates user

---

## 📌 API Endpoints

### 🔓 Public APIs

| Method | Endpoint                             | Description           |
| ------ | ------------------------------------ | --------------------- |
| GET    | `/auth/user/welcome`                 | Health check          |
| POST   | `/auth/user/register-request`        | Send OTP              |
| POST   | `/auth/user/register-user`           | Verify OTP & register |
| POST   | `/auth/user/login`                   | Login                 |
| POST   | `/auth/user/forgot-password/request` | Send OTP              |
| POST   | `/auth/user/forgot-password/verify`  | Verify OTP            |
| POST   | `/auth/user/forgot-password/reset`   | Reset password        |

---

### 🔒 Protected APIs

| Method | Endpoint                            | Description         |
| ------ | ----------------------------------- | ------------------- |
| GET    | `/auth/user/{email}`                | Get user by email   |
| GET    | `/auth/user/all`                    | Get all users       |
| POST   | `/auth/user/update-profile/{email}` | Update profile      |
| POST   | `/auth/user/verify-email-update`    | Verify email change |
| DELETE | `/auth/user/deactivate/{id}`        | Deactivate user     |

---

## ⚙️ Configuration

### `application.yml`

```yaml
server:
  port: ${PORT:8081}

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 3600000

spring:
  application:
    name: AUTH-SERVICE

  datasource:
    url: jdbc:h2:mem:authservice
    driver-class-name: org.h2.Driver
    username: sa
    password: ""

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

📧 Email Setup (Gmail SMTP)

1. Enable 2-Step Verification in Gmail
2. Generate App Password
3. Set environment variables:
   MAIL_USERNAME=your_email@gmail.com
   MAIL_PASSWORD=your_app_password
