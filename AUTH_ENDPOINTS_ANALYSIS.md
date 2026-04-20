# Authentication Requirements Analysis

## PUBLIC ENDPOINTS (✅ No JWT Token Required)

### 1. **Health Check**

- **Endpoint:** `GET /auth/user/welcome`
- **Authentication:** ❌ NOT required
- **Purpose:** Check if service is running
- **Example:**
  ```
  GET http://localhost:8081/auth/user/welcome
  ```
- **Response:** `"welcome! It is working."`

---

## REGISTRATION & LOGIN FLOW (✅ No JWT Token Required)

### 2. **Register Request (Send OTP)**

- **Endpoint:** `POST /auth/user/register-request`
- **Authentication:** ❌ NOT required
- **Purpose:** New user submits registration details and receives OTP on email
- **Request Body:**
  ```json
  {
    "fullName": "John Doe",
    "email": "john@example.com",
    "password": "securepass123",
    "phone": "9876543210"
  }
  ```
- **Response:** `"OTP sent to your email. Valid for 10 minutes."`

### 3. **Verify OTP & Create Account**

- **Endpoint:** `POST /auth/user/register-user?email=john@example.com&otp=123456`
- **Authentication:** ❌ NOT required
- **Purpose:** User verifies OTP received in email and account is created
- **Response:** `{ "token": "eyJhbGc...", "message": "User registered successfully" }`

### 4. **Login User**

- **Endpoint:** `POST /auth/user/login`
- **Authentication:** ❌ NOT required
- **Purpose:** Existing user logs in with email and password
- **Request Body:**
  ```json
  {
    "email": "john@example.com",
    "password": "securepass123"
  }
  ```
- **Response:** `{ "token": "eyJhbGc...", "message": "Login successful" }`

---

## FORGOT PASSWORD FLOW (✅ No JWT Token Required)

### 5. **Request Password Reset OTP**

- **Endpoint:** `POST /auth/user/forgot-password/request?email=john@example.com`
- **Authentication:** ❌ NOT required
- **Purpose:** User requests OTP to reset forgotten password
- **Response:** `"OTP sent to john@example.com"`

### 6. **Verify Password Reset OTP**

- **Endpoint:** `POST /auth/user/forgot-password/verify?email=john@example.com&otp=123456`
- **Authentication:** ❌ NOT required
- **Purpose:** Verify OTP for password reset
- **Response:** `"OTP verified. You can now reset your password."`

### 7. **Reset Password**

- **Endpoint:** `POST /auth/user/forgot-password/reset?email=john@example.com&newPassword=newpass123`
- **Authentication:** ❌ NOT required
- **Purpose:** Set new password after OTP verification
- **Response:** `"Password reset successful"`

---

## PROTECTED ENDPOINTS (🔒 JWT Token REQUIRED)

### 8. **Get User by Email**

- **Endpoint:** `GET /auth/user/{email}`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Fetch user details by email
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Example:**
  ```
  GET http://localhost:8081/auth/user/john@example.com
  Header: Authorization: Bearer <JWT_TOKEN>
  ```
- **Response:**
  ```json
  {
    "userId": 1,
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "9876543210",
    "role": "USER",
    "isActive": true,
    "department": "IT"
  }
  ```

### 9. **Get All Users (Debug)**

- **Endpoint:** `GET /auth/user/all`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Admin/Debug endpoint to list all users
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Response:**
  ```json
  [
    { "userId": 1, "fullName": "John Doe", ... },
    { "userId": 2, "fullName": "Jane Smith", ... }
  ]
  ```

### 10. **Update Profile**

- **Endpoint:** `POST /auth/user/update-profile/{email}`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Update user profile information
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Request Body:**
  ```json
  {
    "fullName": "John Updated",
    "email": "newemail@example.com",
    "phone": "9876543211"
  }
  ```
- **Note:** If email is changed, account becomes INACTIVE until new email is verified via OTP

### 11. **Verify Email Update**

- **Endpoint:** `POST /auth/user/verify-email-update?currentEmail=john@example.com&otp=123456`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Verify OTP for email change (after updating profile)
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Response:** `"Email verified and updated successfully"`

### 12. **Deactivate User Account**

- **Endpoint:** `DELETE /auth/user/deactivate/{id}`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Deactivate/Delete user account
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Example:**
  ```
  DELETE http://localhost:8081/auth/user/deactivate/1
  Header: Authorization: Bearer <JWT_TOKEN>
  ```
- **Response:** `204 No Content`

### 13. **Logout**

- **Endpoint:** `POST /auth/user/logout`
- **Authentication:** 🔒 **REQUIRED**
- **Purpose:** Logout user and invalidate session
- **Header Required:**
  ```
  Authorization: Bearer eyJhbGc...
  ```
- **Response:** `"Logged out successfully"`

---

## Summary Table

| #   | Endpoint                   | Method | Auth | Purpose                     |
| --- | -------------------------- | ------ | ---- | --------------------------- |
| 0   | `/welcome`                 | GET    | ❌   | Health check                |
| 1   | `/register-request`        | POST   | ❌   | Send registration OTP       |
| 2   | `/register-user`           | POST   | ❌   | Verify OTP & create account |
| 3   | `/login`                   | POST   | ❌   | User login                  |
| 4   | `/forgot-password/request` | POST   | ❌   | Request password reset OTP  |
| 5   | `/forgot-password/verify`  | POST   | ❌   | Verify password reset OTP   |
| 6   | `/forgot-password/reset`   | POST   | ❌   | Reset password              |
| 7   | `/{email}`                 | GET    | 🔒   | Get user by email           |
| 8   | `/all`                     | GET    | 🔒   | Get all users (debug)       |
| 9   | `/update-profile/{email}`  | POST   | 🔒   | Update profile              |
| 10  | `/verify-email-update`     | POST   | 🔒   | Verify email change         |
| 11  | `/deactivate/{id}`         | DELETE | 🔒   | Deactivate account          |
| 12  | `/logout`                  | POST   | 🔒   | Logout user                 |

---

## How to Test Protected Endpoints

### Step 1: Register User

```bash
POST http://localhost:8081/auth/user/register-request
Body: { "fullName": "Test User", "email": "test@example.com", "password": "test123", "phone": "9876543210" }
```

### Step 2: Verify OTP & Get Token

```bash
POST http://localhost:8081/auth/user/register-user?email=test@example.com&otp=<OTP_FROM_EMAIL>
Response: { "token": "eyJhbGc...", ... }
```

### Step 3: Use Token for Protected Endpoints

```bash
GET http://localhost:8081/auth/user/test@example.com
Header: Authorization: Bearer eyJhbGc...
```

---

## Security Configuration

All endpoints follow this rule in `SecurityConfig.java`:

- **Public:** `/auth/user/welcome`, `/register-*`, `/login`, `/forgot-password/**`
- **Protected:** All other `/auth/user/**` endpoints require valid JWT token
- **Anonymous requests** to protected endpoints will receive **403 Forbidden** response
