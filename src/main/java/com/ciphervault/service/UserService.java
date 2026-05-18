package com.ciphervault.service;

import com.ciphervault.model.User;
import com.ciphervault.repository.UserRepository;
import com.ciphervault.util.AesEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AesEncryptionUtil aesEncryptionUtil;

    /**
     * Registers a new user. Hashes password, encrypts master key.
     */
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered.");
        }

        // Generate a unique master key per user (used for AES encryption of their vault)
        String rawMasterKey = username + "_" + System.currentTimeMillis() + "_CV_SECRET";
        String encryptedMasterKey = aesEncryptionUtil.encrypt(rawMasterKey);

        User user = User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(password))
            .masterKey(encryptedMasterKey)
            .build();

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}
