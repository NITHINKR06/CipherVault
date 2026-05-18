package com.ciphervault.service;

import com.ciphervault.model.User;
import com.ciphervault.model.VaultEntry;
import com.ciphervault.repository.VaultEntryRepository;
import com.ciphervault.util.AesEncryptionUtil;
import com.ciphervault.util.PasswordStrengthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VaultService {

    @Autowired private VaultEntryRepository vaultEntryRepository;
    @Autowired private AesEncryptionUtil aesEncryptionUtil;
    @Autowired private PasswordStrengthUtil passwordStrengthUtil;
    @Autowired private BreachCheckService breachCheckService;

    /**
     * Add a new vault entry. Encrypts credentials before saving.
     */
    public VaultEntry addEntry(User user, String title, String siteUrl,
                                String rawUsername, String rawPassword,
                                String notes, String category) {
        int strength = passwordStrengthUtil.calculateStrength(rawPassword);

        // Check breach status
        int breachCount = breachCheckService.checkPasswordBreach(rawPassword);

        VaultEntry entry = VaultEntry.builder()
            .user(user)
            .title(title)
            .siteUrl(siteUrl)
            .usernameEnc(aesEncryptionUtil.encrypt(rawUsername))
            .passwordEnc(aesEncryptionUtil.encrypt(rawPassword))
            .notesEnc(notes != null && !notes.isEmpty() ? aesEncryptionUtil.encrypt(notes) : null)
            .category(category != null ? category : "General")
            .strengthScore(strength)
            .isBreached(breachCount > 0)
            .breachCount(breachCount)
            .build();

        return vaultEntryRepository.save(entry);
    }

    /**
     * Returns all entries for a user (encrypted — decryption is on demand).
     */
    public List<VaultEntry> getEntries(User user) {
        return vaultEntryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Decrypt a single entry's password. Called only when user explicitly reveals it.
     */
    public String decryptPassword(Long entryId, User user) {
        VaultEntry entry = vaultEntryRepository.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return aesEncryptionUtil.decrypt(entry.getPasswordEnc());
    }

    /**
     * Decrypt a single entry's username.
     */
    public String decryptUsername(Long entryId, User user) {
        VaultEntry entry = vaultEntryRepository.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return aesEncryptionUtil.decrypt(entry.getUsernameEnc());
    }

    /**
     * Update a vault entry.
     */
    public VaultEntry updateEntry(Long entryId, User user, String title, String siteUrl,
                                   String rawUsername, String rawPassword,
                                   String notes, String category) {
        VaultEntry entry = vaultEntryRepository.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        int strength = passwordStrengthUtil.calculateStrength(rawPassword);
        int breachCount = breachCheckService.checkPasswordBreach(rawPassword);

        entry.setTitle(title);
        entry.setSiteUrl(siteUrl);
        entry.setUsernameEnc(aesEncryptionUtil.encrypt(rawUsername));
        entry.setPasswordEnc(aesEncryptionUtil.encrypt(rawPassword));
        entry.setNotesEnc(notes != null && !notes.isEmpty() ? aesEncryptionUtil.encrypt(notes) : null);
        entry.setCategory(category);
        entry.setStrengthScore(strength);
        entry.setBreached(breachCount > 0);
        entry.setBreachCount(breachCount);

        return vaultEntryRepository.save(entry);
    }

    /**
     * Delete a vault entry.
     */
    public void deleteEntry(Long entryId, User user) {
        VaultEntry entry = vaultEntryRepository.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        vaultEntryRepository.delete(entry);
    }

    /**
     * Get dashboard stats for the user.
     */
    public Map<String, Object> getDashboardStats(User user) {
        long total = vaultEntryRepository.countByUser(user);
        long breached = vaultEntryRepository.countBreachedByUser(user);
        Double avgStrength = vaultEntryRepository.avgStrengthByUser(user);

        return Map.of(
            "total", total,
            "breached", breached,
            "safe", total - breached,
            "avgStrength", avgStrength != null ? Math.round(avgStrength) : 0
        );
    }

    public Optional<VaultEntry> findById(Long id) {
        return vaultEntryRepository.findById(id);
    }
}
