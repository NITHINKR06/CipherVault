# 🛠️ CipherVault — Tools & Technology Decisions

> A deep-dive into every tool, library, and technology used in CipherVault — what it does, why it was chosen, and what was considered as an alternative.

---

## Table of Contents

1. [Backend Framework — Spring Boot](#1-backend-framework--spring-boot)
2. [Language — Java 17](#2-language--java-17)
3. [Security Framework — Spring Security](#3-security-framework--spring-security)
4. [Password Hashing — BCrypt](#4-password-hashing--bcrypt)
5. [Encryption — AES-256-GCM](#5-encryption--aes-256-gcm)
6. [Database — MySQL 8](#6-database--mysql-8)
7. [ORM — Spring Data JPA / Hibernate](#7-orm--spring-data-jpa--hibernate)
8. [Templating Engine — Thymeleaf](#8-templating-engine--thymeleaf)
9. [Frontend — HTML5, CSS3, Vanilla JavaScript](#9-frontend--html5-css3-vanilla-javascript)
10. [UI Library — Bootstrap 5.3 + Bootstrap Icons](#10-ui-library--bootstrap-53--bootstrap-icons)
11. [Breach Intelligence API — HaveIBeenPwned (k-Anonymity)](#11-breach-intelligence-api--haveibeenpwned-k-anonymity)
12. [Build Tool — Maven](#12-build-tool--maven)
13. [Boilerplate Reducer — Lombok](#13-boilerplate-reducer--lombok)
14. [Input Validation — Spring Boot Validation (Bean Validation / Jakarta)](#14-input-validation--spring-boot-validation-bean-validation--jakarta)
15. [Architecture Pattern — MVC (Model-View-Controller)](#15-architecture-pattern--mvc-model-view-controller)

---

## 1. Backend Framework — Spring Boot

### What it is
Spring Boot is a Java-based framework that simplifies building production-ready web applications. It wraps the larger Spring Framework and auto-configures most setup so you can focus on business logic.

### What it does in CipherVault
- Hosts the entire web server (embedded Tomcat)
- Routes HTTP requests to controllers (`AuthController`, `VaultController`)
- Manages the application lifecycle, dependency injection, and component wiring
- Connects all layers: security, database, service logic, and templates

### Why Spring Boot over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Jakarta EE (plain)** | Requires an external server (Tomcat/WildFly), more boilerplate setup, heavier configuration |
| **Quarkus** | Excellent for containers/native builds, but smaller ecosystem and less mature Spring Security equivalent |
| **Micronaut** | Good for microservices, but Spring's ecosystem for security + JPA + Thymeleaf is more battle-tested |
| **Django (Python)** | Would require switching language entirely; Java gives stronger type safety for crypto operations |
| **Express.js (Node)** | Interpreted language; Spring's compile-time type checking is safer for a security-critical app |

**Why Spring Boot wins:** It has the most mature, production-hardened ecosystem for security-critical applications. Spring Security, Spring Data JPA, and Thymeleaf all integrate seamlessly out of the box.

---

## 2. Language — Java 17

### What it is
Java 17 is a Long-Term Support (LTS) release of Java, the most widely used language for enterprise and backend development.

### What it does in CipherVault
- Powers all backend logic: encryption, business rules, database interaction, request handling
- Provides strong typing, which prevents whole classes of bugs in security-sensitive code
- Java 17 introduces sealed classes, pattern matching, and modern records — used via Lombok and Spring

### Why Java 17 over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Java 11** | Older LTS; misses text blocks, records, and some crypto improvements in JDK 17 |
| **Java 21** | Very recent at time of development; Spring Boot 3.2 targets Java 17 as minimum |
| **Kotlin** | Excellent JVM language, but the team/developer is working in standard Java; interop overhead not needed |
| **Python** | Dynamically typed; type errors in crypto code are harder to catch at compile time |
| **Go** | Strong for systems programming, but weaker Spring-equivalent ecosystem |

**Why Java 17:** It is the recommended minimum for Spring Boot 3.x, offers modern language features, and has the JCA (Java Cryptography Architecture) built in — used directly for AES-256-GCM.

---

## 3. Security Framework — Spring Security

### What it is
Spring Security is the de-facto standard security framework for Java applications. It handles authentication (who are you?), authorization (what can you do?), session management, CSRF protection, and more.

### What it does in CipherVault
- Manages user login and logout flows
- Protects all `/vault/**` routes — only authenticated users can access them
- Enforces CSRF tokens on all form submissions (prevents cross-site request forgery)
- Integrates with `CustomUserDetailsService` to load users from the MySQL database
- Redirects unauthenticated users to the login page automatically

### Why Spring Security over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Manual session management** | Error-prone; easy to introduce authentication bypass bugs |
| **Apache Shiro** | Less integration with Spring ecosystem; Spring Security is the native choice |
| **JWT-only auth** | Stateless JWT is better for APIs/microservices; for a server-rendered app, session-based auth is simpler and more secure |
| **OAuth2 only** | Would require a third-party identity provider; CipherVault keeps auth self-contained for privacy |

**Why Spring Security:** It is battle-tested by millions of applications, integrates natively with Spring Boot, and provides CSRF protection, session fixation protection, and secure logout with almost zero extra code.

---

## 4. Password Hashing — BCrypt

### What it is
BCrypt is an adaptive, one-way password hashing algorithm designed specifically for hashing passwords. It incorporates a "cost factor" that controls how computationally expensive the hash is to compute.

### What it does in CipherVault
- Hashes master passwords before storing them in the database
- Cost factor of 12 is used — making a brute-force attack extremely slow
- When a user logs in, Spring Security compares the incoming password against the stored BCrypt hash
- Hashing is one-way: neither the developer nor the server can recover the original password

### Why BCrypt over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **MD5 / SHA-1 / SHA-256** | Fast hashing algorithms — fast is bad for passwords; trivially brute-forced with GPUs |
| **PBKDF2** | Acceptable, but BCrypt is more widely considered the standard for password hashing at this stack |
| **Argon2** | The modern gold standard, but Spring Security's BCrypt support is more mature and simpler to configure |
| **Plain text / Base64** | Never acceptable — database compromise would expose all passwords |

**Why BCrypt:** It is natively supported by Spring Security, widely trusted, and the cost factor makes it resistant to brute-force attacks even if the database is stolen.

---

## 5. Encryption — AES-256-GCM

### What it is
AES-256-GCM (Advanced Encryption Standard, 256-bit key, Galois/Counter Mode) is a symmetric encryption algorithm. It provides both **confidentiality** (data is secret) and **authenticity** (data has not been tampered with).

### What it does in CipherVault
- Encrypts stored usernames and passwords in `VaultEntry` before database write
- Uses a unique random IV (Initialization Vector) per encryption — same plaintext gives different ciphertext every time
- GCM's authentication tag detects if ciphertext is tampered with after storage
- Decrypts credentials only when the user explicitly requests to view them
- Implemented in `AesEncryptionUtil.java` using Java's built-in JCA (`javax.crypto`)

### Why AES-256-GCM over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **AES-256-CBC** | No built-in authentication — vulnerable to padding oracle attacks without extra MAC |
| **AES-128** | Smaller key size; 256-bit is preferred for security-critical credential storage |
| **RSA** | Asymmetric — designed for key exchange, not bulk data encryption; much slower |
| **ChaCha20-Poly1305** | Excellent, but less universally supported across Java versions; GCM is the JCA standard |
| **No encryption (hashing only)** | Hashing is one-way; credentials need to be retrieved and shown to the user — so reversible encryption is required |

**Why AES-256-GCM:** It is the industry standard for authenticated symmetric encryption, natively available in Java's JCA with no extra dependencies, and is recommended by NIST for data-at-rest protection.

---

## 6. Database — MySQL 8

### What it is
MySQL 8 is a widely-used open-source relational database management system (RDBMS).

### What it does in CipherVault
- Stores all application data: users, vault entries (encrypted), and audit logs
- Three core tables: `users`, `vault_entries`, `audit_logs`
- Schema is defined in `sql/schema.sql` for reproducible setup

### Why MySQL over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **PostgreSQL** | Excellent alternative; MySQL chosen for wider familiarity and easier local setup for most developers |
| **SQLite** | Not suitable for multi-user or production deployments; single-file DB has concurrency limits |
| **MongoDB** | Document DB; no benefit for this structured, relational data model (users → entries → logs) |
| **H2 (in-memory)** | Good for testing, not for persistent credential storage |
| **MariaDB** | MySQL-compatible fork; either works, MySQL chosen as the reference implementation |

**Why MySQL 8:** It is reliable, widely supported by cloud providers, and integrates seamlessly with Spring Data JPA. Its strong ACID guarantees are important for a credential storage application.

---

## 7. ORM — Spring Data JPA / Hibernate

### What it is
Spring Data JPA is an abstraction over JPA (Java Persistence API), with Hibernate as the underlying ORM (Object-Relational Mapper). It maps Java objects to database tables and lets you query the database without writing raw SQL in most cases.

### What it does in CipherVault
- Maps `User`, `VaultEntry`, and `AuditLog` Java classes to their respective MySQL tables
- Repository interfaces (`UserRepository`, `VaultEntryRepository`, `AuditLogRepository`) provide CRUD methods with zero boilerplate
- Handles connection pooling, transactions, and query generation automatically

### Why Spring Data JPA over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Plain JDBC** | Requires manual SQL, ResultSet parsing, and connection management — high boilerplate, error-prone |
| **MyBatis** | More explicit SQL mapping — good for complex queries, but overkill here where queries are simple |
| **jOOQ** | Type-safe SQL generation — excellent but heavier setup for a project of this scope |
| **Raw Hibernate** | Spring Data JPA wraps Hibernate with less boilerplate; no reason to use raw Hibernate here |

**Why Spring Data JPA:** It eliminates repetitive database code, integrates with Spring transactions and Spring Security, and is the standard choice for Spring Boot applications.

---

## 8. Templating Engine — Thymeleaf

### What it is
Thymeleaf is a Java server-side templating engine that processes HTML templates and injects dynamic data from the backend before sending the page to the browser.

### What it does in CipherVault
- Renders all HTML pages: `landing.html`, `login.html`, `register.html`, `vault.html`, `add-entry.html`, `edit-entry.html`
- Injects authenticated user data, vault entries, and error messages into pages server-side
- Uses `thymeleaf-extras-springsecurity6` to show/hide elements based on authentication state (e.g., show "Logout" only when logged in)
- Processes form bindings and CSRF tokens automatically

### Why Thymeleaf over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **JSP (JavaServer Pages)** | Legacy technology; poor IDE support, mixing Java in HTML is messy |
| **FreeMarker** | Good alternative, but less Spring Boot integration and community support than Thymeleaf |
| **React / Vue / Angular SPA** | A full SPA would require a separate API backend; adds complexity for a single-developer project |
| **Mustache** | Too minimal — lacks the Spring Security integration and form binding Thymeleaf provides |

**Why Thymeleaf:** It is the officially recommended templating engine for Spring Boot, supports natural HTML (templates are valid HTML files), and has first-class Spring Security integration for role-based rendering.

---

## 9. Frontend — HTML5, CSS3, Vanilla JavaScript

### What it is
The standard web trio: HTML5 for structure, CSS3 for styling, and JavaScript (no framework) for client-side interactivity.

### What it does in CipherVault
- **HTML5** structures all pages with semantic elements (`<form>`, `<table>`, `<input>`, etc.)
- **CSS3** (`style.css`) powers the full dark-mode UI with custom variables, animations, and layout
- **JavaScript** (`vault.js`) handles:
  - Live password strength meter (scored 0–100)
  - One-click password generation
  - Reveal/hide password toggle (decrypt on demand)
  - Copy-to-clipboard with 30-second auto-clear
  - AJAX requests for breach checking without full page reload
  - Search/filter filtering vault entries client-side

### Why Vanilla JS over a framework

| Alternative | Why NOT chosen |
|---|---|
| **React** | Overkill for a multi-page server-rendered app; adds build tooling complexity |
| **Vue.js** | Same — a reactive framework is unnecessarily heavy for small client-side interactions |
| **jQuery** | Adds a dependency for things modern JS handles natively (`fetch`, `querySelector`, etc.) |
| **Alpine.js** | Reasonable lightweight alternative, but vanilla JS keeps zero external JS dependencies |

**Why Vanilla JS:** The client-side logic in CipherVault is focused and contained. Vanilla JS keeps the project dependency-free on the frontend, with no build pipeline needed.

---

## 10. UI Library — Bootstrap 5.3 + Bootstrap Icons

### What it is
Bootstrap is a CSS/JS component library that provides pre-built responsive grid, form, button, modal, and table components. Bootstrap Icons is a companion SVG icon library.

### What it does in CipherVault
- Provides the responsive grid layout (works on mobile and desktop)
- Styles forms, buttons, tables, cards, badges, and modals
- Bootstrap Icons used for lock icons, eye (reveal), clipboard, trash, edit, and shield symbols throughout the UI

### Why Bootstrap over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Tailwind CSS** | Utility-first; requires more markup and a build step (PostCSS/npm) for full use |
| **Materialize** | Google Material Design — fine aesthetically, but less community momentum than Bootstrap today |
| **Bulma** | Good lightweight alternative, but fewer ready-made components and no JS included |
| **Custom CSS only** | Possible, but Bootstrap handles responsive breakpoints and cross-browser consistency for free |

**Why Bootstrap 5.3:** It is the fastest path to a polished, accessible, mobile-responsive UI with no build tooling. Bootstrap 5 (no jQuery dependency) is modern and widely understood.

---

## 11. Breach Intelligence API — HaveIBeenPwned (k-Anonymity)

### What it is
HaveIBeenPwned (HIBP) is a public service by Troy Hunt that aggregates data from billions of leaked credentials across hundreds of known data breaches. Its **Pwned Passwords API** accepts partial SHA-1 hashes and returns matching breach records.

The **k-Anonymity model** ensures the full password (or full hash) is never sent to the API.

### What it does in CipherVault
- When a password is added or viewed, `BreachCheckService.java` checks it against the HIBP database
- The SHA-1 hash of the password is computed locally
- Only the **first 5 characters** of the hash are sent to HIBP's API
- HIBP returns ~500 hash suffixes that start with those 5 characters
- The full hash is matched **locally** — HIBP never learns what password was checked
- The vault dashboard displays a breach warning badge on any compromised credential

### Why HaveIBeenPwned over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **DeHashed API** | Paid service; HIBP Pwned Passwords is free and has the largest password breach dataset |
| **SpyCloud** | Enterprise-only, expensive |
| **Building your own breach database** | Impractical — HIBP has 10+ billion breached passwords; downloading and hosting this locally is not feasible |
| **Skipping breach checking entirely** | Would remove a key differentiating security feature |

**Why HIBP k-Anonymity:** It is free, covers the largest known breach dataset (10+ billion passwords), and the k-Anonymity model means zero privacy risk — the password or its full hash never leaves CipherVault's server.

---

## 12. Build Tool — Maven

### What it is
Maven is a Java build automation and dependency management tool. It uses a `pom.xml` (Project Object Model) file to declare dependencies, plugins, and build configuration.

### What it does in CipherVault
- Declares all project dependencies (Spring Boot, MySQL, Lombok, etc.) in `pom.xml`
- Downloads dependencies from Maven Central automatically
- Compiles the project, runs tests, and packages it into a runnable JAR with `mvn clean install`
- The Spring Boot Maven Plugin enables `mvn spring-boot:run` for local development

### Why Maven over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Gradle** | More flexible and faster for large multi-module builds; Maven is simpler for a single-module Spring Boot project |
| **Ant** | Legacy; no dependency management built in |
| **Bazel** | Built for massive monorepos; excessive complexity for this project |

**Why Maven:** Spring Boot's official project generator (start.spring.io) defaults to Maven, it has excellent Spring Boot plugin support, and it is the most familiar build tool for Java developers.

---

## 13. Boilerplate Reducer — Lombok

### What it is
Project Lombok is a Java annotation processor that auto-generates repetitive code (getters, setters, constructors, `toString`, `equals`, `hashCode`) at compile time via annotations like `@Data`, `@Getter`, `@NoArgsConstructor`.

### What it does in CipherVault
- Eliminates getters/setters on `User`, `VaultEntry`, and `AuditLog` model classes
- `@NoArgsConstructor` / `@AllArgsConstructor` generate constructors automatically
- Keeps model classes lean and readable — the actual fields and JPA annotations stand out clearly

### Why Lombok over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Java Records** | Immutable by design; JPA entities need mutable fields and a no-arg constructor — Records don't fit the JPA model |
| **Manual getters/setters** | Verbose and distracting; doubles the line count of model files with zero logic |
| **MapStruct only** | MapStruct handles DTO mapping, not entity boilerplate reduction |

**Why Lombok:** It is the standard boilerplate-reduction tool in the Spring Boot ecosystem, widely supported by IDEs, and integrates transparently with JPA and Spring.

---

## 14. Input Validation — Spring Boot Validation (Bean Validation / Jakarta)

### What it is
Spring Boot Validation integrates the Jakarta Bean Validation API (formerly `javax.validation`), allowing you to annotate model fields with constraints like `@NotBlank`, `@Size`, `@Email`, and have Spring validate them automatically on form submission.

### What it does in CipherVault
- Validates user registration input: non-empty username, minimum password length, valid email format
- Validates vault entry input: title and password are required
- Returns validation error messages back to Thymeleaf templates without custom error-handling code

### Why Spring Boot Validation over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Manual if/else validation** | Repetitive, error-prone, hard to maintain |
| **Client-side JS validation only** | Never sufficient — client-side validation can be bypassed; server-side is mandatory |
| **Custom validator classes** | Fine for complex business rules, but annotation-based validation handles common cases with zero boilerplate |

**Why Spring Boot Validation:** It is built into Spring Boot with one dependency, integrates directly with Thymeleaf form error display, and follows the standard Jakarta Bean Validation spec.

---

## 15. Architecture Pattern — MVC (Model-View-Controller)

### What it is
MVC is a software design pattern that separates an application into three interconnected components: Model (data), View (UI), and Controller (logic that connects them).

### How it maps to CipherVault

| Layer | Component | Files |
|---|---|---|
| **Model** | Data entities | `User.java`, `VaultEntry.java`, `AuditLog.java` |
| **View** | Thymeleaf HTML templates | `vault.html`, `login.html`, etc. |
| **Controller** | Request handlers | `AuthController.java`, `VaultController.java` |
| **Service** | Business logic (between Controller and Model) | `VaultService.java`, `BreachCheckService.java`, etc. |
| **Repository** | Database access layer | `UserRepository.java`, `VaultEntryRepository.java`, etc. |

### Why MVC over alternatives

| Alternative | Why NOT chosen |
|---|---|
| **Everything in one class** | Unmanageable for anything beyond a toy project |
| **Hexagonal / Clean Architecture** | More suitable for large enterprise systems; adds structural complexity not needed here |
| **Event-driven / CQRS** | Excellent for high-scale distributed systems; overkill for a personal password manager |

**Why MVC:** It is the natural pattern for Spring Boot + Thymeleaf applications, universally understood by Java developers, and cleanly separates concerns — making the codebase easy to navigate, test, and extend.

---

## Summary Table

| Tool / Technology | Category | Purpose in CipherVault | Key Reason Chosen |
|---|---|---|---|
| Spring Boot 3.2 | Backend Framework | Web server, routing, DI | Mature ecosystem, zero-config security + JPA |
| Java 17 | Language | All backend logic | LTS, Spring Boot 3.x requirement, JCA crypto |
| Spring Security | Security Framework | Auth, session, CSRF protection | Native Spring integration, battle-tested |
| BCrypt (factor 12) | Password Hashing | Master password storage | Adaptive, one-way, Spring native support |
| AES-256-GCM | Encryption | Credential encryption at rest | Authenticated encryption, NIST standard |
| MySQL 8 | Database | Persistent data storage | Reliable RDBMS, ACID, JPA-compatible |
| Spring Data JPA / Hibernate | ORM | Database access layer | Zero-boilerplate repositories |
| Thymeleaf | Templating | Server-rendered HTML pages | Spring Security integration, natural HTML |
| HTML5 / CSS3 / JS | Frontend | UI structure, styling, interactions | No build tooling needed, Thymeleaf-compatible |
| Bootstrap 5.3 | UI Library | Responsive components & layout | Fastest path to polished responsive UI |
| Bootstrap Icons | Icons | UI iconography | Zero-dependency SVG icons, Bootstrap ecosystem |
| HaveIBeenPwned API | Breach Intelligence | Real-time breach detection | Free, largest dataset, k-Anonymity privacy |
| Maven | Build Tool | Dependency management, build | Spring Boot default, simple single-module project |
| Lombok | Code Generation | Reduce entity boilerplate | Standard Spring ecosystem tool |
| Bean Validation (Jakarta) | Input Validation | Form field validation | Annotation-based, Thymeleaf error integration |

---

> *Every tool in CipherVault was chosen to solve a specific problem with the least complexity, the best security posture, and the strongest integration with the Spring ecosystem.*
