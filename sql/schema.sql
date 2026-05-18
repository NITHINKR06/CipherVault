-- ============================================
-- CipherVault Database Schema
-- ============================================

CREATE DATABASE IF NOT EXISTS ciphervault;
USE ciphervault;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,         -- BCrypt hashed
    master_key  VARCHAR(255) NOT NULL,         -- AES key (encrypted)
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login  TIMESTAMP NULL
);

-- Vault Entries Table (all secrets stored AES-encrypted)
CREATE TABLE IF NOT EXISTS vault_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(255) NOT NULL,
    site_url        VARCHAR(500),
    username_enc    TEXT         NOT NULL,     -- AES encrypted
    password_enc    TEXT         NOT NULL,     -- AES encrypted
    notes_enc       TEXT,                     -- AES encrypted
    category        VARCHAR(50)  DEFAULT 'General',
    strength_score  INT          DEFAULT 0,   -- 0-100
    is_breached     BOOLEAN      DEFAULT FALSE,
    breach_count    INT          DEFAULT 0,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Breach Check Logs
CREATE TABLE IF NOT EXISTS breach_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    entry_id    BIGINT,
    checked_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    is_breached BOOLEAN     DEFAULT FALSE,
    breach_count INT        DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entry_id) REFERENCES vault_entries(id) ON DELETE SET NULL
);

-- Audit Logs (who accessed what and when)
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    action      VARCHAR(100) NOT NULL,        -- LOGIN, VIEW, ADD, UPDATE, DELETE
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    timestamp   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_vault_user ON vault_entries(user_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_breach_user ON breach_logs(user_id);
