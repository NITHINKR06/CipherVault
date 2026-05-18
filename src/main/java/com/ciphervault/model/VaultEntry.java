package com.ciphervault.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * VaultEntry entity — stores encrypted credentials.
 * username_enc and password_enc are AES-256 encrypted blobs.
 * Even if the DB is dumped, data is unreadable without the user's master key.
 */
@Entity
@Table(name = "vault_entries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VaultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "site_url", length = 500)
    private String siteUrl;

    @Column(name = "username_enc", nullable = false, columnDefinition = "TEXT")
    private String usernameEnc;     // AES encrypted

    @Column(name = "password_enc", nullable = false, columnDefinition = "TEXT")
    private String passwordEnc;     // AES encrypted

    @Column(name = "notes_enc", columnDefinition = "TEXT")
    private String notesEnc;        // AES encrypted

    @Column(length = 50)
    @Builder.Default
    private String category = "General";

    @Column(name = "strength_score")
    @Builder.Default
    private int strengthScore = 0;  // 0–100

    @Column(name = "is_breached")
    @Builder.Default
    private boolean isBreached = false;

    @Column(name = "breach_count")
    @Builder.Default
    private int breachCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
