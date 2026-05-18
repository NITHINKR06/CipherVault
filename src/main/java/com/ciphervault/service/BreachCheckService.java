package com.ciphervault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * BreachCheckService — checks if a password has appeared in known data breaches.
 *
 * Uses HaveIBeenPwned k-Anonymity API:
 * 1. SHA-1 hash the password
 * 2. Send only first 5 chars of hash to HIBP (privacy preserved!)
 * 3. HIBP returns all hash suffixes matching that prefix
 * 4. Check if our full hash suffix is in the response
 *
 * This means HIBP never sees the actual password.
 */
@Service
public class BreachCheckService {

    @Value("${ciphervault.hibp.api.url}")
    private String hibpApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns how many times this password appeared in breach databases.
     * Returns 0 if not found (safe).
     */
    public int checkPasswordBreach(String password) {
        try {
            // SHA-1 hash the password
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
            String fullHash = HexFormat.of().formatHex(hashBytes).toUpperCase();

            String prefix = fullHash.substring(0, 5);
            String suffix = fullHash.substring(5);

            // Query HIBP with only the prefix (k-Anonymity model)
            String url = hibpApiUrl + prefix;
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) return 0;

            // Parse response to find our suffix
            for (String line : response.split("\n")) {
                String[] parts = line.trim().split(":");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                    return Integer.parseInt(parts[1].trim());
                }
            }

            return 0; // Not found in breaches
        } catch (Exception e) {
            // If HIBP is unreachable, fail open (don't block the user)
            return -1; // -1 means check failed
        }
    }
}
