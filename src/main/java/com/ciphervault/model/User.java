package com.ciphervault.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User entity — stores account credentials and master encryption key.
 * Password is BCrypt hashed. masterKey is AES-encrypted.
 */
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;        // BCrypt hashed

    @Column(name = "master_key", nullable = false)
    private String masterKey;       // AES key (encrypted at rest)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VaultEntry> vaultEntries;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
