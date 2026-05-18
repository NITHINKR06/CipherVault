package com.ciphervault.util;

import org.springframework.stereotype.Component;

/**
 * PasswordStrengthUtil — scores password strength from 0 to 100.
 *
 * Scoring logic:
 * - Length                 : up to 30 points
 * - Uppercase letters      : 10 points
 * - Lowercase letters      : 10 points
 * - Digits                 : 15 points
 * - Special characters     : 20 points
 * - No common patterns     : 15 points
 */
@Component
public class PasswordStrengthUtil {

    private static final String[] COMMON_PATTERNS = {
        "password", "123456", "qwerty", "abc123", "letmein",
        "monkey", "1111", "admin", "welcome", "login"
    };

    public int calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        int score = 0;

        // Length scoring (max 30)
        int len = password.length();
        if (len >= 8)  score += 10;
        if (len >= 12) score += 10;
        if (len >= 16) score += 10;

        // Character type scoring
        if (password.matches(".*[A-Z].*"))               score += 10; // Uppercase
        if (password.matches(".*[a-z].*"))               score += 10; // Lowercase
        if (password.matches(".*[0-9].*"))               score += 15; // Digits
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score += 20; // Special

        // No common patterns (15 points)
        boolean hasCommon = false;
        String lower = password.toLowerCase();
        for (String pattern : COMMON_PATTERNS) {
            if (lower.contains(pattern)) {
                hasCommon = true;
                break;
            }
        }
        if (!hasCommon) score += 15;

        return Math.min(score, 100);
    }

    public String getStrengthLabel(int score) {
        if (score < 20)  return "Very Weak";
        if (score < 40)  return "Weak";
        if (score < 60)  return "Fair";
        if (score < 80)  return "Strong";
        return "Very Strong";
    }

    public String getStrengthColor(int score) {
        if (score < 20)  return "#ff2d55";
        if (score < 40)  return "#ff6b35";
        if (score < 60)  return "#ffd60a";
        if (score < 80)  return "#30d158";
        return "#00d4ff";
    }
}
