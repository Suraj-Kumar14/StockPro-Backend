# Role Management Quick Reference

## 🔑 4 Roles in Your Inventory Management System

### **Role Assignment Diagram**
```
User Registration/OAuth2 Login
              ↓
    WAREHOUSE_STAFF (DEFAULT)
              ↓
      (Only Admin can promote)
              ↓
    ┌─────────┴─────────┬──────────────┐
    ↓                   ↓              ↓
INVENTORY_MANAGER  PURCHASE_OFFICER  ADMIN
(Inventory Module)  (Purchase Module) (Full Access)
```

---

## Role Details Table

| Role | Default | Can Access | Typical User |
|------|---------|-----------|--------------|
| **WAREHOUSE_STAFF** | ✅ Yes | /warehouse/* | Warehouse Employee |
| **INVENTORY_MANAGER** | ❌ No | /inventory/* | Inventory Head |
| **PURCHASE_OFFICER** | ❌ No | /purchase/* | Purchase Manager |
| **ADMIN** | ❌ No | All endpoints | System Administrator |

---

## 🔐 Endpoint Access Matrix

```yaml
PUBLIC ENDPOINTS (No login required):
  /auth/user/welcome
  /auth/user/register-request
  /auth/user/register-user
  /auth/user/login
  /auth/user/forgot-password/**
  /oauth2/authorization/google    ← NEW!
  /login/oauth2/code/google        ← NEW!
  /swagger-ui/**
  /h2-console/**
  /actuator/**

AUTHENTICATED ENDPOINTS (Any logged-in user):
  /auth/user/{email}
  /auth/user/all
  /auth/user/update-profile/**
  /auth/user/verify-email-update
  /auth/user/logout

ROLE-SPECIFIC ENDPOINTS:
  /inventory/**            → ADMIN + INVENTORY_MANAGER only
  /warehouse/**            → ADMIN + WAREHOUSE_STAFF only
  /purchase/**             → ADMIN + PURCHASE_OFFICER only
  /auth/admin/**           → ADMIN only
```

---

## 📋 User Journey by Role

### **Warehouse Staff (Default Role)**
```
1. User registers via email OR Google OAuth2
2. Automatically gets WAREHOUSE_STAFF role
3. Can access: /warehouse/* endpoints
4. Cannot access: /inventory/*, /purchase/*, /auth/admin/*
5. Can only modify own profile, cannot manage other users
```

### **Inventory Manager (Promoted by Admin)**
```
1. Admin promotes WAREHOUSE_STAFF user to INVENTORY_MANAGER
2. Now can access: /inventory/* endpoints
3. Cannot access: /warehouse/*, /purchase/*, /auth/admin/*
4. Manages inventory operations
```

### **Purchase Officer (Promoted by Admin)**
```
1. Admin promotes WAREHOUSE_STAFF user to PURCHASE_OFFICER
2. Now can access: /purchase/* endpoints
3. Cannot access: /warehouse/*, /inventory/*, /auth/admin/*
4. Manages purchase operations
```

### **Admin (Assigned by Database)**
```
1. Only created by direct database manipulation or super-admin
2. Can access: ALL endpoints
3. Can: Manage users, deactivate accounts, change user roles
4. Should be: System administrator account
```

---

## 🔄 Role Transition Flow

```
New User (Email/OAuth2)
         ↓
    WAREHOUSE_STAFF
    ↙    ↓    ↘
   ❌   ✓    ❌
   |    |    |
ADMIN  KEEP  Cannot change
      itself  by own
      
Admin can promote to:
  → INVENTORY_MANAGER
  → PURCHASE_OFFICER  
  → ADMIN
  
Admin can demote to:
  → WAREHOUSE_STAFF (lowest)
```

---

## 🛡️ Security Rules

### **Role Hierarchy** (Admin > Others)
```
ADMIN
  ├─ Can do anything
  ├─ INVENTORY_MANAGER capabilities
  ├─ WAREHOUSE_STAFF capabilities
  └─ PURCHASE_OFFICER capabilities

INVENTORY_MANAGER
  ├─ Can access /inventory/*
  └─ Cannot promote themselves
  
WAREHOUSE_STAFF
  ├─ Can access /warehouse/*
  └─ Cannot promote themselves
  
PURCHASE_OFFICER
  ├─ Can access /purchase/*
  └─ Cannot promote themselves
```

### **What Users CAN'T Do**
- ❌ Change their own role
- ❌ Promote/demote other users
- ❌ Access endpoints outside their role
- ❌ Reactivate deactivated accounts
- ❌ View other users' passwords

### **What ADMIN Can Do**
- ✅ Change any user's role
- ✅ Deactivate any user
- ✅ View all user information
- ✅ Access all endpoints
- ✅ Manage system settings

---

## 📊 Department vs Role

### **Department** (User's Team)
```yaml
User.department = "WAREHOUSE" | "INVENTORY" | "PURCHASE" | "GENERAL"
Purpose: Track which team user belongs to
Used by: Admin for reporting and organization
```

### **Role** (Access Level)
```yaml
User.role = "WAREHOUSE_STAFF" | "INVENTORY_MANAGER" | "PURCHASE_OFFICER" | "ADMIN"
Purpose: Control what user can access
Used by: SecurityConfig for authorization
```

**Example:**
```
User: Ravi Kumar
  Department: WAREHOUSE
  Role: WAREHOUSE_STAFF
  Access: /warehouse/* endpoints
  
After Promotion:
  Department: WAREHOUSE (unchanged)
  Role: INVENTORY_MANAGER
  Access: /inventory/* endpoints
```

---

## 🔍 Debugging Role Issues

### **Check User's Current Role**
```sql
SELECT email, role, department, isActive FROM users WHERE email='ravi@example.com';
```

### **Update User Role** (Database - Admin only)
```sql
UPDATE users SET role='INVENTORY_MANAGER' WHERE email='ravi@example.com';
```

### **View All Users & Roles**
```sql
SELECT userId, email, fullName, role, department, isActive FROM users;
```

### **Check Token Contains Role** (JWT Payload)
```javascript
// After login, JWT token contains:
{
  "sub": "ravi@example.com",     // User email
  "userId": 1,                    // User ID
  "iat": 1234567890,              // Issued at
  "exp": 1234571490               // Expiration
  // Note: Role is in User entity, not in JWT
}
```

---

## 🚀 Quick Setup for Different Scenarios

### **Scenario 1: Add New Employee**
```
1. Employee registers via email or Google OAuth2
   → Gets WAREHOUSE_STAFF role automatically
   → Can access /warehouse/* endpoints
2. Done! Ready to work
```

### **Scenario 2: Promote Employee to Manager**
```
1. Admin calls Admin-Service endpoint: PUT /admin/user/{id}/role
   → Payload: { "newRole": "INVENTORY_MANAGER" }
2. Employee now can access /inventory/* endpoints
3. Done!
```

### **Scenario 3: Deactivate Employee**
```
1. Admin calls: DELETE /auth/admin/deactivate/{id}
2. Employee's isActive = false
3. Employee cannot login anymore
4. Admin can reactivate by: UPDATE users SET isActive=true
```

### **Scenario 4: Google OAuth2 New User**
```
1. User clicks "Login with Google"
2. Google redirects to /oauth2/authorization/google
3. After Google auth, Spring redirects to /login/oauth2/code/google
4. OAuth2LoginSuccessHandler creates user with WAREHOUSE_STAFF role
5. Frontend receives token + role in URL
6. User is logged in!
```

---

## 📝 Configuration Checklist

- [ ] Set JWT_SECRET in environment variables
- [ ] Set JWT_EXPIRATION_MS to desired time
- [ ] Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
- [ ] Set MAIL_USERNAME and MAIL_PASSWORD for email
- [ ] Set EUREKA_URL to Eureka server address
- [ ] Test all 4 roles have correct access
- [ ] Verify OAuth2 login works
- [ ] Check email OTP sending works
- [ ] Verify role-based endpoint access

---

## 🎯 Role Usage Examples

### **API Call: Get Inventory Data** (INVENTORY_MANAGER only)
```bash
curl -X GET http://localhost:8081/inventory/items \
  -H "Authorization: Bearer <jwt-token>"
  # Token must be from user with INVENTORY_MANAGER or ADMIN role
```

### **API Call: Get Warehouse Data** (WAREHOUSE_STAFF + ADMIN)
```bash
curl -X GET http://localhost:8081/warehouse/stock \
  -H "Authorization: Bearer <jwt-token>"
  # Token must be from user with WAREHOUSE_STAFF or ADMIN role
```

### **API Call: Get Purchase Data** (PURCHASE_OFFICER + ADMIN)
```bash
curl -X GET http://localhost:8081/purchase/orders \
  -H "Authorization: Bearer <jwt-token>"
  # Token must be from user with PURCHASE_OFFICER or ADMIN role
```

### **API Call: Admin Deactivate User** (ADMIN only)
```bash
curl -X DELETE http://localhost:8081/auth/admin/deactivate/123 \
  -H "Authorization: Bearer <admin-jwt-token>"
  # Token must be from user with ADMIN role
```

---

## ✨ Summary

Your Auth-Service now supports:
- ✅ 4 distinct roles with clear permissions
- ✅ Email/Password authentication
- ✅ Google OAuth2 login
- ✅ Automatic role assignment for new users
- ✅ Admin-controlled role promotion
- ✅ Department tracking for organization
- ✅ OTP-based verification
- ✅ JWT token-based authorization
- ✅ Comprehensive logging
- ✅ Security best practices

**Remember:** Every new user (email or OAuth2) gets WAREHOUSE_STAFF role by default. Only ADMIN can promote users to other roles!

