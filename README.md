# 🔐 CipherVault — Personal Secrets Manager with Breach Intelligence

> A full-stack cybersecurity web application built with Java Spring Boot, MySQL, HTML5, CSS3, JavaScript, and Bootstrap.
> Stores credentials with **AES-256-GCM encryption** and checks every password against real breach databases using **k-Anonymity**.

---

## 📁 Project Structure

```
CipherVault/
├── pom.xml                                         # Maven dependencies
├── sql/
│   └── schema.sql                                  # Database schema
└── src/main/
    ├── java/com/ciphervault/
    │   ├── CipherVaultApplication.java             # App entry point
    │   ├── config/
    │   │   ├── SecurityConfig.java                 # Spring Security config
    │   │   └── CustomUserDetailsService.java       # Auth user loader
    │   ├── controller/
    │   │   ├── AuthController.java                 # Login / Register
    │   │   └── VaultController.java                # Vault CRUD + AJAX
    │   ├── model/
    │   │   ├── User.java                           # User entity
    │   │   ├── VaultEntry.java                     # Encrypted credential entity
    │   │   └── AuditLog.java                       # Activity log entity
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── VaultEntryRepository.java
    │   │   └── AuditLogRepository.java
    │   ├── service/
    │   │   ├── UserService.java                    # Registration, auth helpers
    │   │   ├── VaultService.java                   # Encrypt/decrypt vault logic
    │   │   ├── BreachCheckService.java             # HaveIBeenPwned k-Anonymity
    │   │   └── AuditLogService.java                # Activity tracking
    │   └── util/
    │       ├── AesEncryptionUtil.java              # AES-256-GCM engine
    │       └── PasswordStrengthUtil.java           # Strength scoring
    └── resources/
        ├── application.properties                  # Config (DB, AES key)
        ├── templates/                              # Thymeleaf HTML pages
        │   ├── landing.html
        │   ├── login.html
        │   ├── register.html
        │   ├── vault.html
        │   ├── add-entry.html
        │   └── edit-entry.html
        └── static/
            ├── css/style.css                       # Full dark UI stylesheet
            └── js/vault.js                         # Client-side JS
```

---

## ⚙️ Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 17, Spring Boot 3.2            |
| Security   | Spring Security, BCrypt, AES-256-GCM|
| Database   | MySQL 8+, Spring Data JPA           |
| Frontend   | HTML5, CSS3, JavaScript (Vanilla)   |
| UI Library | Bootstrap 5.3, Bootstrap Icons      |
| Templates  | Thymeleaf                           |
| Breach API | HaveIBeenPwned (k-Anonymity model)  |

---

## 🚀 Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+

### Step 1 — Database Setup
```sql
-- Run this in MySQL Workbench or CLI
source sql/schema.sql;
```

### Step 2 — Configure application.properties
```properties
# Edit src/main/resources/application.properties
spring.datasource.username=YOUR_MYSQL_USER
spring.datasource.password=YOUR_MYSQL_PASSWORD
ciphervault.aes.secret=ChangeThisToA32CharSecretKey!123
```

### Step 3 — Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

### Step 4 — Open Browser
```
http://localhost:8080
```

---

## 🔒 Security Architecture

### AES-256-GCM Encryption
- Every username and password is encrypted **before** touching the database
- Uses a unique random IV per encryption — same input gives different ciphertext
- GCM mode provides authenticated encryption (tamper detection built-in)
- Even if the database is stolen, all data is unreadable ciphertext

### HaveIBeenPwned — k-Anonymity
```
1. SHA-1 hash the password
2. Send only first 5 chars of hash to HIBP API
3. HIBP returns ~500 hash suffixes matching that prefix
4. Match locally — HIBP never sees the actual password
```

### BCrypt Password Hashing
- Master passwords hashed with BCrypt (strength factor 12)
- Not reversible — even the developer cannot see your master password

### Audit Trail
- Every login, view, add, update, delete is logged with timestamp + IP
- Users can review their own activity history in the dashboard

---

## 🎯 Features

| Feature                    | Description                                          |
|----------------------------|------------------------------------------------------|
| 🔐 AES-256-GCM Encryption  | Credentials encrypted at rest with unique IV         |
| 💀 Breach Detection        | Real-time check via HIBP k-Anonymity API             |
| 📊 Strength Scoring        | Live password strength meter (0–100)                 |
| ⚡ Password Generator      | One-click 18-char strong password generation         |
| 👁️ Reveal on Demand       | Secrets only decrypted when explicitly clicked       |
| 📋 Clipboard Copy          | Copy password, auto-clears clipboard in 30s          |
| 🔍 Search & Filter         | Filter by title, category, breach status             |
| 📝 Audit Logs              | Full activity history with IP tracking               |
| 🏷️ Categories             | Organize by Social, Banking, Work, Email, etc.       |
| 📱 Responsive              | Works on mobile and desktop                          |

---

## 📸 Pages

| Route           | Page              |
|-----------------|-------------------|
| `/`             | Landing page      |
| `/register`     | Create vault      |
| `/login`        | Sign in           |
| `/vault`        | Main dashboard    |
| `/vault/add`    | Add new secret    |
| `/vault/edit/{id}` | Edit secret    |

---

## 🧪 How to Explain This (for Interviews / Hackathons)

> *"CipherVault is a full-stack password manager where even the developer cannot read your credentials. Every secret is encrypted with AES-256-GCM using a unique initialization vector before hitting the database — so a raw database dump reveals nothing useful. For breach detection, I implemented HaveIBeenPwned's k-Anonymity model: we hash the password with SHA-1, send only the first 5 characters of that hash to their API, and match the result locally. The actual password never leaves our server in any identifiable form. All user actions are logged in an audit trail, and master passwords are BCrypt hashed with a cost factor of 12."*

---

## 🛡️ Built By
CipherVault — because your secrets deserve better than plaintext.
