package com.ciphervault.controller;

import com.ciphervault.model.User;
import com.ciphervault.model.VaultEntry;
import com.ciphervault.service.*;
import com.ciphervault.util.PasswordStrengthUtil;import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * VaultController — handles all vault operations.
 */
@Controller
@RequestMapping("/vault")
public class VaultController {

    @Autowired private VaultService vaultService;
    @Autowired private UserService userService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private PasswordStrengthUtil passwordStrengthUtil;
    @Autowired private BreachCheckService breachCheckService;

    // ─── Dashboard ───────────────────────────────────────────────
    @GetMapping
    public String vaultDashboard(Principal principal, Model model, HttpServletRequest request) {
        User user = getUser(principal);
        userService.updateLastLogin(principal.getName());

        List<VaultEntry> entries = vaultService.getEntries(user);
        Map<String, Object> stats = vaultService.getDashboardStats(user);
        List<com.ciphervault.model.AuditLog> logs = auditLogService.getRecentLogs(user);

        model.addAttribute("entries", entries);
        model.addAttribute("stats", stats);
        model.addAttribute("auditLogs", logs);
        model.addAttribute("user", user);
        model.addAttribute("strengthUtil", passwordStrengthUtil);

        auditLogService.log(user, "VIEWED VAULT", request);
        return "vault";
    }

    // ─── Add Entry Page ───────────────────────────────────────────
    @GetMapping("/add")
    public String addEntryPage(Model model, Principal principal) {
        model.addAttribute("user", getUser(principal));
        return "add-entry";
    }

    // ─── Add Entry Submit ─────────────────────────────────────────
    @PostMapping("/add")
    public String addEntry(@RequestParam String title,
                            @RequestParam(required = false) String siteUrl,
                            @RequestParam String entryUsername,
                            @RequestParam String entryPassword,
                            @RequestParam(required = false) String notes,
                            @RequestParam(required = false) String category,
                            Principal principal,
                            RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        User user = getUser(principal);
        try {
            VaultEntry entry = vaultService.addEntry(user, title, siteUrl, entryUsername, entryPassword, notes, category);
            auditLogService.log(user, "ADDED ENTRY: " + title, request);

            if (entry.isBreached()) {
                redirectAttributes.addFlashAttribute("warning",
                    "⚠️ Entry saved, but this password was found in " + entry.getBreachCount() + " data breaches!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Entry added securely to your vault.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add entry: " + e.getMessage());
        }
        return "redirect:/vault";
    }

    // ─── Edit Entry Page ──────────────────────────────────────────
    @GetMapping("/edit/{id}")
    public String editEntryPage(@PathVariable Long id, Principal principal, Model model) {
        User user = getUser(principal);
        VaultEntry entry = vaultService.findById(id)
            .orElseThrow(() -> new RuntimeException("Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            return "redirect:/vault";
        }

        // Decrypt for editing
        String decUsername = vaultService.decryptUsername(id, user);
        String decPassword = vaultService.decryptPassword(id, user);

        model.addAttribute("entry", entry);
        model.addAttribute("decUsername", decUsername);
        model.addAttribute("decPassword", decPassword);
        model.addAttribute("user", user);
        return "edit-entry";
    }

    // ─── Edit Entry Submit ────────────────────────────────────────
    @PostMapping("/edit/{id}")
    public String editEntry(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam(required = false) String siteUrl,
                             @RequestParam String entryUsername,
                             @RequestParam String entryPassword,
                             @RequestParam(required = false) String notes,
                             @RequestParam(required = false) String category,
                             Principal principal,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        User user = getUser(principal);
        try {
            VaultEntry entry = vaultService.updateEntry(id, user, title, siteUrl, entryUsername, entryPassword, notes, category);
            auditLogService.log(user, "UPDATED ENTRY: " + title, request);

            if (entry.isBreached()) {
                redirectAttributes.addFlashAttribute("warning",
                    "⚠️ Updated, but password found in " + entry.getBreachCount() + " breaches!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Entry updated successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update: " + e.getMessage());
        }
        return "redirect:/vault";
    }

    // ─── Delete Entry ─────────────────────────────────────────────
    @PostMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id, Principal principal,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {
        User user = getUser(principal);
        try {
            vaultService.deleteEntry(id, user);
            auditLogService.log(user, "DELETED ENTRY #" + id, request);
            redirectAttributes.addFlashAttribute("success", "Entry deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete entry.");
        }
        return "redirect:/vault";
    }

    // ─── Reveal Password (AJAX) ───────────────────────────────────
    @GetMapping("/reveal/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> revealPassword(@PathVariable Long id,
                                                               Principal principal,
                                                               HttpServletRequest request) {
        User user = getUser(principal);
        try {
            String decPassword = vaultService.decryptPassword(id, user);
            String decUsername = vaultService.decryptUsername(id, user);
            auditLogService.log(user, "REVEALED ENTRY #" + id, request);
            return ResponseEntity.ok(Map.of("password", decPassword, "username", decUsername));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Quick Breach Check (AJAX) ────────────────────────────────
    @PostMapping("/check-breach")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkBreach(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password required"));
        }
        int score  = passwordStrengthUtil.calculateStrength(password);
        String label = passwordStrengthUtil.getStrengthLabel(score);
        int breachCount = breachCheckService.checkPasswordBreach(password);

        return ResponseEntity.ok(Map.of(
            "strength",     score,
            "strengthLabel", label,
            "breached",     breachCount > 0,
            "breachCount",  breachCount
        ));
    }

    // ─── Helper ───────────────────────────────────────────────────
    private User getUser(Principal principal) {
        return userService.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
