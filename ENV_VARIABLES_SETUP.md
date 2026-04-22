# Auth-Service Environment Variables Setup 🔧

## Overview
Your Auth-Service uses environment variables for sensitive configuration. This ensures secrets are NOT hardcoded in the codebase.

---

## All Required Environment Variables

### **1. JWT Configuration**
```bash
JWT_SECRET=ThisIsASecretKeyForJwtTokenGenerationThatMustBeAtLeast32BytesLong12345
JWT_EXPIRATION_MS=3600000
```
**Purpose:**
- `JWT_SECRET` - Used to sign and validate JWT tokens (minimum 32 bytes)
- `JWT_EXPIRATION_MS` - How long JWT token is valid in milliseconds (3600000 = 1 hour)

**How to generate a secure JWT_SECRET:**
```bash
# On Linux/Mac:
openssl rand -base64 32

# On Windows (PowerShell):
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

---

### **2. Google OAuth2 Configuration**
```bash
GOOGLE_CLIENT_ID=your-client-id-123456789.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret-abc123xyz789
REDIRECT_URI=http://localhost:8081/login/oauth2/code/google
```

**Purpose:**
- `GOOGLE_CLIENT_ID` - Identifies your application to Google
- `GOOGLE_CLIENT_SECRET` - Authenticates your application to Google (KEEP SECRET!)
- `REDIRECT_URI` - Where Google redirects after user authenticates

**How to get these credentials:**
```
1. Go to: https://console.cloud.google.com
2. Create new project: "Inventory Management System"
3. Enable: Google+ API
4. Create OAuth 2.0 credential (Web application)
5. Add authorized redirect URI: http://localhost:8081/login/oauth2/code/google
6. Copy Client ID and Client Secret
7. For production, add your domain instead of localhost
```

**Important Notes:**
- For **DEVELOPMENT:** Use `http://localhost:8081/...`
- For **PRODUCTION:** Use your actual domain: `https://yourdomain.com/...`
- Never commit GOOGLE_CLIENT_SECRET to version control
- Different credentials needed for each environment (dev, staging, prod)

---

### **3. Email Service Configuration (SMTP - Gmail)**
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password-16-chars
```

**Purpose:**
- Sending OTP emails for verification
- Sending password reset codes
- Sending email update verification codes

**How to set up Gmail SMTP:**

#### **Step 1: Enable 2FA on Gmail Account**
```
1. Go to: https://myaccount.google.com/security
2. Find "2-Step Verification" section
3. Click "Get Started"
4. Follow the process to enable 2FA
```

#### **Step 2: Generate App Password**
```
1. Go to: https://myaccount.google.com/security
2. Find "App passwords" (appears only if 2FA is enabled)
3. Select: Mail → Windows Computer (or your OS)
4. Google generates 16-character password
5. Copy this password (spaces will be removed automatically)
```

#### **Step 3: Add Credentials to Auth-Service**
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxxxx  # 16-character app password from step 2
```

**Example Email Configuration in application.yml** (for reference):
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

---

### **4. Eureka Configuration (Service Discovery)**
```bash
EUREKA_URL=http://localhost:8761/eureka
```

**Purpose:**
- Registers Auth-Service with Eureka Service Registry
- Allows other services to discover Auth-Service

**For Development:**
```bash
EUREKA_URL=http://localhost:8761/eureka
```

**For Production:**
```bash
EUREKA_URL=http://eureka-server.yourdomain.com:8761/eureka
```

---

### **5. Admin Server Configuration**
```bash
ADMIN_SERVER_URL=http://localhost:8080
```

**Purpose:**
- Auth-Service registers with Spring Boot Admin for monitoring

**For Development:**
```bash
ADMIN_SERVER_URL=http://localhost:8080
```

**For Production:**
```bash
ADMIN_SERVER_URL=https://admin.yourdomain.com
```

---

### **6. Frontend URL Configuration**
```bash
FRONTEND_URL=http://localhost:4200
```

**Purpose:**
- Used for redirects after OAuth2 authentication
- Used for error redirects
- Frontend receives JWT token and role in URL parameters

**For Development:**
```bash
FRONTEND_URL=http://localhost:4200
```

**For Production:**
```bash
FRONTEND_URL=https://yourdomain.com
```

---

### **7. Server Port Configuration**
```bash
PORT=8081
```

**Purpose:**
- Which port the Auth-Service runs on

**Default:** 8081
**Can change to:** Any available port

---

## Complete Environment Setup Examples

### **Development Environment** (Local Machine)
Create a `.env` file:
```bash
# JWT
JWT_SECRET=YourSecureSecretKeyHere1234567890123456789012345678
JWT_EXPIRATION_MS=3600000

# Google OAuth2
GOOGLE_CLIENT_ID=123456789-abcdefghijklmnopqrstuvwxyz123.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-YourSecretHereXYZ123456
REDIRECT_URI=http://localhost:8081/login/oauth2/code/google

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=aaaa bbbb cccc dddd

# Eureka
EUREKA_URL=http://localhost:8761/eureka

# Admin Server
ADMIN_SERVER_URL=http://localhost:8080

# Frontend
FRONTEND_URL=http://localhost:4200

# Port
PORT=8081
```

---

### **Production Environment** (Server Deployment)
Set environment variables on your server:
```bash
export JWT_SECRET=YourProductionSecretKeyHere1234567890123456
export JWT_EXPIRATION_MS=3600000
export GOOGLE_CLIENT_ID=prod-client-id.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=GOCSPX-ProdSecretXYZ
export REDIRECT_URI=https://yourdomain.com/login/oauth2/code/google
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=noreply@yourdomain.com
export MAIL_PASSWORD=production-app-password
export EUREKA_URL=http://eureka-server.internal:8761/eureka
export ADMIN_SERVER_URL=https://admin.yourdomain.com
export FRONTEND_URL=https://yourdomain.com
export PORT=8081
```

---

## How to Set Environment Variables

### **Option 1: Linux/Mac Terminal**
```bash
# Temporary (current session only)
export JWT_SECRET=your-secret-key
export GOOGLE_CLIENT_ID=your-client-id
# ... set other variables

# Permanent (add to ~/.bashrc or ~/.zshrc)
echo 'export JWT_SECRET=your-secret-key' >> ~/.bashrc
source ~/.bashrc
```

### **Option 2: Windows PowerShell**
```powershell
# Temporary (current session only)
$env:JWT_SECRET="your-secret-key"
$env:GOOGLE_CLIENT_ID="your-client-id"
# ... set other variables

# Permanent (User Environment Variables)
# Control Panel → System → Advanced → Environment Variables
# Add each variable with its value
```

### **Option 3: Docker** (.env file)
```bash
# Create .env file in project root
JWT_SECRET=your-secret-key
GOOGLE_CLIENT_ID=your-client-id
# ... other variables

# Docker Compose reads .env automatically
docker-compose up
```

### **Option 4: application-{profile}.yml**
```yaml
# application-prod.yml
app:
  jwt:
    secret: ${JWT_SECRET:default-secret}
    expiration-ms: ${JWT_EXPIRATION_MS:3600000}

spring:
  mail:
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
```

Then run with profile:
```bash
java -jar auth-service.jar --spring.profiles.active=prod
```

---

## Verification: Are Variables Loaded?

### **Check in Application Logs**
```
INFO  DiscoveryClient : registering service...
INFO  OAuth2LoginSuccessHandler : ...
INFO  EmailService : OTP email sent...
```

If you see these messages, environment variables are loaded correctly.

### **Check OAuth2 is Configured**
```bash
curl http://localhost:8081/oauth2/authorization/google
# Should redirect to Google login page
```

### **Check Email Service**
Register a test user and check if OTP email is received.

---

## Security Best Practices 🔒

### **DO:**
✅ Use strong, random values for JWT_SECRET
✅ Use App Passwords instead of main Gmail password
✅ Store secrets in environment variables (never in code)
✅ Use HTTPS in production for OAuth2 redirects
✅ Rotate secrets periodically
✅ Use different credentials for dev/staging/prod
✅ Keep GOOGLE_CLIENT_SECRET confidential
✅ Enable 2FA on Gmail account

### **DON'T:**
❌ Commit .env files to Git
❌ Use simple/predictable secret values
❌ Share GOOGLE_CLIENT_SECRET publicly
❌ Use HTTP in production
❌ Use same credentials across environments
❌ Log sensitive values
❌ Hardcode secrets in application.yml
❌ Use main Gmail password for SMTP

---

## Troubleshooting

### **Problem: "Invalid Google Client ID"**
**Solution:**
1. Verify GOOGLE_CLIENT_ID is set and correct
2. Check Google Cloud Console for correct credentials
3. Ensure OAuth2 API is enabled
4. Verify redirect URI matches exactly

### **Problem: "OTP Email not sending"**
**Solution:**
1. Verify MAIL_USERNAME and MAIL_PASSWORD are correct
2. Check 2FA is enabled on Gmail
3. Verify App Password was generated (not regular password)
4. Check firewall allows outbound SMTP (port 587)
5. Review email service logs: `SMTP not configured`

### **Problem: "JWT token invalid"**
**Solution:**
1. Verify JWT_SECRET is same on startup
2. Check JWT_SECRET length (minimum 32 bytes)
3. Ensure token includes "Bearer " prefix
4. Verify token hasn't expired (check JWT_EXPIRATION_MS)

### **Problem: "OAuth2 redirect fails"**
**Solution:**
1. Verify REDIRECT_URI matches Google Cloud configuration exactly
2. Verify FRONTEND_URL is correct (used for error redirects)
3. Check browser allows redirects
4. Enable developer tools to debug redirect

---

## Testing with Real Values

After setting all environment variables, test with:

```bash
# 1. Start Auth-Service
cd auth-service
mvn spring-boot:run

# 2. Test welcome endpoint
curl http://localhost:8081/auth/user/welcome

# 3. Register user (will send OTP email)
curl -X POST http://localhost:8081/auth/user/register-request \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "password": "Test123!",
    "phone": "9876543210",
    "department": "WAREHOUSE"
  }'

# 4. Check email for OTP
# 5. Verify with OTP
curl -X POST "http://localhost:8081/auth/user/register-user?email=test@example.com&otp=123456"

# 6. Login
curl -X POST http://localhost:8081/auth/user/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "Test123!"}'

# 7. Try Google OAuth2
# Open browser: http://localhost:8081/oauth2/authorization/google
```

---

## Environment Variables Summary Table

| Variable | Purpose | Required | Example |
|----------|---------|----------|---------|
| JWT_SECRET | Signing JWT tokens | ✅ Yes | `YourSecureKeyHere123...` |
| JWT_EXPIRATION_MS | Token validity (ms) | ✅ Yes | `3600000` (1 hour) |
| GOOGLE_CLIENT_ID | Google OAuth app ID | ✅ Yes | `123456789.apps.googleusercontent.com` |
| GOOGLE_CLIENT_SECRET | Google OAuth secret | ✅ Yes | `GOCSPX-ABC123...` |
| REDIRECT_URI | OAuth2 callback URL | ✅ Yes | `http://localhost:8081/...` |
| MAIL_HOST | SMTP server | ✅ Yes | `smtp.gmail.com` |
| MAIL_PORT | SMTP port | ✅ Yes | `587` |
| MAIL_USERNAME | Email account | ✅ Yes | `your-email@gmail.com` |
| MAIL_PASSWORD | Email app password | ✅ Yes | `aaaa bbbb cccc dddd` |
| EUREKA_URL | Service registry | ✅ Yes | `http://localhost:8761/eureka` |
| ADMIN_SERVER_URL | Admin dashboard | ✅ Yes | `http://localhost:8080` |
| FRONTEND_URL | Frontend base URL | ✅ Yes | `http://localhost:4200` |
| PORT | Service port | ❌ No | `8081` |

---

## Quick Start (Copy-Paste Ready)

### **For Linux/Mac Development:**
```bash
# JWT Configuration
export JWT_SECRET="YourDevelopmentSecretKeyHere123456789"
export JWT_EXPIRATION_MS="3600000"

# Google OAuth2
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export REDIRECT_URI="http://localhost:8081/login/oauth2/code/google"

# Email
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"

# Services
export EUREKA_URL="http://localhost:8761/eureka"
export ADMIN_SERVER_URL="http://localhost:8080"
export FRONTEND_URL="http://localhost:4200"

# Port
export PORT="8081"

# Start service
cd auth-service
mvn spring-boot:run
```

### **For Windows PowerShell Development:**
```powershell
# JWT Configuration
$env:JWT_SECRET="YourDevelopmentSecretKeyHere123456789"
$env:JWT_EXPIRATION_MS="3600000"

# Google OAuth2
$env:GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:REDIRECT_URI="http://localhost:8081/login/oauth2/code/google"

# Email
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-app-password"

# Services
$env:EUREKA_URL="http://localhost:8761/eureka"
$env:ADMIN_SERVER_URL="http://localhost:8080"
$env:FRONTEND_URL="http://localhost:4200"

# Port
$env:PORT="8081"

# Start service
cd auth-service
mvn spring-boot:run
```

---

All set! Your Auth-Service is ready to run with proper environment configuration! 🎉

