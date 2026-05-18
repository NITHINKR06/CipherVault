# 🔐 CipherVault

> **Personal Secrets Manager with Breach Intelligence**

A full-stack cybersecurity web application that stores your credentials with **AES-256-GCM encryption** and checks every password against real-world breach databases in real-time using the **k-Anonymity model** — so your actual password never leaves the server.

---

## 🧠 What is CipherVault?

CipherVault is a self-hosted password vault built for developers and security-conscious users. Unlike basic password managers, it goes a step further by integrating with the **HaveIBeenPwned (HIBP)** breach database to alert you if any of your stored passwords have been leaked in known data breaches — all without revealing your password to the API.

Every credential is encrypted **before** it touches the database. Even if the database is compromised, all stored data is unreadable ciphertext.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 AES-256-GCM Encryption | Every username and password is encrypted at rest with a unique random IV |
| 💀 Breach Detection | Real-time HIBP k-Anonymity check — your password never leaves the server |
| 📊 Strength Scoring | Live password strength meter scored from 0–100 |
| ⚡ Password Generator | One-click generation of strong 18-character passwords |
| 👁️ Reveal on Demand | Secrets are only decrypted when explicitly clicked — not exposed by default |
| 📋 Clipboard Copy | Copy passwords with auto-clear after 30 seconds |
| 🔍 Search & Filter | Filter entries by title, category, or breach status |
| 📝 Audit Logs | Full activity history with timestamps and IP tracking |
| 🏷️ Categories | Organize credentials by Social, Banking, Work, Email, and more |
| 📱 Responsive UI | Works seamlessly on both mobile and desktop |

---

## 🛡️ Security Architecture

### AES-256-GCM Encryption

Every stored username and password is encrypted **before** being written to the database using AES-256-GCM. A unique random IV (Initialization Vector) is generated for each encryption operation — meaning the same input produces a different ciphertext every time. GCM mode also provides **authenticated encryption**, meaning any tampering with stored ciphertext is detectable.

```
Plaintext → AES-256-GCM (random IV) → Ciphertext (stored in DB)
```

Even a full database dump is useless without the AES secret key.

---

### HaveIBeenPwned — k-Anonymity

The breach check is done without ever sending your password to any external service:

```
1. SHA-1 hash the password locally
2. Send only the first 5 characters of the hash to the HIBP API
3. HIBP returns ~500 hash suffixes matching that prefix
4. Match the full hash locally
→ HIBP never sees the actual password or full hash
```

---

### BCrypt Master Password Hashing

Master passwords are hashed using **BCrypt with a cost factor of 12**. These hashes are one-way — not even the server administrator can recover your master password.

---

### Audit Trail

Every user action — login, view, add, edit, delete — is logged with a timestamp and IP address. Users can review their full activity history directly from the dashboard.

---

## ⚙️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Security | Spring Security, BCrypt, AES-256-GCM |
| Database | MySQL 8+, Spring Data JPA |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| UI Library | Bootstrap 5.3, Bootstrap Icons |
| Templating | Thymeleaf |
| Breach API | HaveIBeenPwned (k-Anonymity model) |

> 📖 **Want to know why each tool was chosen over its alternatives?**
> See the full breakdown → [TOOLS.md](./Tools_used.md)

---

## 📁 Project Structure

```
CipherVault/
├── pom.xml                                         # Maven dependencies
├── sql/
│   └── schema.sql                                  # Database schema
└── src/main/
    ├── java/com/ciphervault/
    │   ├── CipherVaultApplication.java             # Application entry point
    │   ├── config/
    │   │   ├── SecurityConfig.java                 # Spring Security configuration
    │   │   └── CustomUserDetailsService.java       # Auth user loader
    │   ├── controller/
    │   │   ├── AuthController.java                 # Login / Register endpoints
    │   │   └── VaultController.java                # Vault CRUD + AJAX endpoints
    │   ├── model/
    │   │   ├── User.java                           # User entity
    │   │   ├── VaultEntry.java                     # Encrypted credential entity
    │   │   └── AuditLog.java                       # Activity log entity
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── VaultEntryRepository.java
    │   │   └── AuditLogRepository.java
    │   ├── service/
    │   │   ├── UserService.java                    # Registration & auth helpers
    │   │   ├── VaultService.java                   # Encrypt/decrypt vault logic
    │   │   ├── BreachCheckService.java             # HIBP k-Anonymity integration
    │   │   └── AuditLogService.java                # Activity tracking
    │   └── util/
    │       ├── AesEncryptionUtil.java              # AES-256-GCM engine
    │       └── PasswordStrengthUtil.java           # Strength scoring logic
    └── resources/
        ├── application.properties                  # App configuration (DB, AES key)
        ├── templates/                              # Thymeleaf HTML pages
        │   ├── landing.html
        │   ├── login.html
        │   ├── register.html
        │   ├── vault.html
        │   ├── add-entry.html
        │   └── edit-entry.html
        └── static/
            ├── css/style.css                       # Dark UI stylesheet
            └── js/vault.js                         # Client-side JavaScript
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+

---

### Step 1 — Database Setup

Run the provided schema in MySQL Workbench or the CLI:

```sql
source sql/schema.sql;
```

---

### Step 2 — Configure `application.properties`

CipherVault reads secrets from environment variables (recommended for Render/Railway).

Set these env vars locally (or in your hosting provider):

```bash
# MySQL
DB_URL=jdbc:mysql://localhost:3306/ciphervault?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=YOUR_MYSQL_USER
DB_PASSWORD=YOUR_MYSQL_PASSWORD

# AES key (use 32+ chars; keep it private)
AES_SECRET=ChangeThisToAStrongSecretKeyAtLeast32Chars
```

> ⚠️ Never commit `.env`, DB credentials, or `AES_SECRET`.

---

### Step 3 — Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

---

### Step 4 — Open in Browser

```
http://localhost:8080
```

---

## 🗺️ Application Routes

| Route | Page |
|---|---|
| `/` | Landing page |
| `/register` | Create a new vault account |
| `/login` | Sign in |
| `/vault` | Main dashboard — view all credentials |
| `/vault/add` | Add a new secret |
| `/vault/edit/{id}` | Edit an existing secret |

---

## 🧪 How It Works (Interview / Hackathon Pitch)

> *"CipherVault is a full-stack password manager where even the developer cannot read your credentials. Every secret is encrypted with AES-256-GCM using a unique initialization vector before hitting the database — so a raw database dump reveals nothing useful. For breach detection, I implemented HaveIBeenPwned's k-Anonymity model: we hash the password with SHA-1, send only the first 5 characters of that hash to their API, and match the result locally. The actual password never leaves our server in any identifiable form. All user actions are logged in an audit trail, and master passwords are BCrypt hashed with a cost factor of 12."*

---

## 🔮 Potential Enhancements

- Two-Factor Authentication (TOTP)
- Export vault to encrypted file
- Browser extension for autofill
- Password expiry reminders
- Email alerts on breach detection
- Docker / Docker Compose support

---

## 👤 Author

Built by [NITHINKR06](https://github.com/NITHINKR06)

---

> *CipherVault — because your secrets deserve better than plaintext.*
