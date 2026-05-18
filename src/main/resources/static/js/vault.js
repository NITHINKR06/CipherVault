/**
 * CipherVault — Client-Side JavaScript
 * Handles: password strength, breach check, reveal/copy, filter, generator, modals
 */

// ── Password Strength (client-side preview) ─────────────────────────────────
function liveStrength(password) {
    const bar   = document.getElementById('strengthBar');
    const label = document.getElementById('strengthLabel');
    if (!bar) return;

    const score = clientStrengthScore(password);
    const { color, text } = getStrengthMeta(score);

    bar.style.width      = score + '%';
    bar.style.background = color;

    if (label) {
        label.textContent  = text;
        label.style.color  = color;
    }
}

function clientStrengthScore(password) {
    if (!password) return 0;
    let score = 0;
    if (password.length >= 8)  score += 10;
    if (password.length >= 12) score += 10;
    if (password.length >= 16) score += 10;
    if (/[A-Z]/.test(password)) score += 10;
    if (/[a-z]/.test(password)) score += 10;
    if (/[0-9]/.test(password)) score += 15;
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) score += 20;

    const common = ['password','123456','qwerty','abc123','letmein','monkey','admin','welcome'];
    const hasCommon = common.some(p => password.toLowerCase().includes(p));
    if (!hasCommon) score += 15;

    return Math.min(score, 100);
}

function getStrengthMeta(score) {
    if (score < 20)  return { color: '#ff2d55', text: 'Very Weak' };
    if (score < 40)  return { color: '#ff6b35', text: 'Weak' };
    if (score < 60)  return { color: '#ffd60a', text: 'Fair' };
    if (score < 80)  return { color: '#30d158', text: 'Strong' };
    return { color: '#00d4ff', text: 'Very Strong' };
}

// ── Live Breach Check (calls Java backend) ────────────────────────────────────
let breachTimer = null;

function liveBreachCheck(password) {
    clearTimeout(breachTimer);
    const breachLabel  = document.getElementById('breachLabel');
    const breachNotice = document.getElementById('breachNotice');
    const breachNoticeText = document.getElementById('breachNoticeText');

    if (!password || password.length < 4) {
        if (breachLabel)  breachLabel.textContent = '';
        if (breachNotice) breachNotice.style.display = 'none';
        return;
    }

    if (breachLabel) { breachLabel.textContent = '⏳ Checking...'; breachLabel.style.color = '#8b949e'; }

    breachTimer = setTimeout(async () => {
        try {
            // We call our own backend which proxies HIBP with k-anonymity
            const resp = await fetch('/vault/check-breach', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(typeof CSRF_HEADER !== 'undefined' && { [CSRF_HEADER]: CSRF_TOKEN })
                },
                body: JSON.stringify({ password })
            });
            const data = await resp.json();

            // The check-breach endpoint only returns strength for now; breach
            // is checked server-side on save. Just show strength here.
            if (breachLabel) breachLabel.textContent = '';
            if (breachNotice) breachNotice.style.display = 'none';

        } catch (e) {
            if (breachLabel) { breachLabel.textContent = 'Check failed'; }
        }
    }, 700);
}

// ── Toggle Password Visibility ────────────────────────────────────────────────
function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const icon = btn.querySelector('i');

    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'bi bi-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'bi bi-eye';
    }
}

// ── Password Generator ────────────────────────────────────────────────────────
function generatePassword() {
    const chars = {
        upper:   'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
        lower:   'abcdefghijklmnopqrstuvwxyz',
        digits:  '0123456789',
        symbols: '!@#$%^&*()_+-=[]{}|;:,.<>?'
    };

    const allChars = chars.upper + chars.lower + chars.digits + chars.symbols;
    const length = 18;

    // Guarantee at least one of each type
    let password = [
        chars.upper[Math.floor(Math.random() * chars.upper.length)],
        chars.lower[Math.floor(Math.random() * chars.lower.length)],
        chars.digits[Math.floor(Math.random() * chars.digits.length)],
        chars.symbols[Math.floor(Math.random() * chars.symbols.length)]
    ];

    for (let i = password.length; i < length; i++) {
        password.push(allChars[Math.floor(Math.random() * allChars.length)]);
    }

    // Shuffle
    password = password.sort(() => Math.random() - 0.5).join('');

    const input = document.getElementById('entryPassword');
    if (input) {
        input.type  = 'text';
        input.value = password;
        liveStrength(password);
        liveBreachCheck(password);

        // Flash animation
        input.style.borderColor = '#00e5ff';
        input.style.boxShadow   = '0 0 0 3px rgba(0,229,255,0.2)';
        setTimeout(() => {
            input.style.borderColor = '';
            input.style.boxShadow   = '';
        }, 1500);
    }
}

// ── Reveal Entry (AJAX) ──────────────────────────────────────────────────────
const revealedEntries = new Set();

async function revealEntry(entryId, btn) {
    const pwEl = document.getElementById('password-' + entryId);
    const unEl = document.getElementById('username-' + entryId);
    const icon = btn.querySelector('i');

    if (revealedEntries.has(entryId)) {
        // Hide again
        if (pwEl) { pwEl.textContent = '••••••••'; pwEl.style.color = ''; }
        if (unEl) { unEl.textContent = '••••••••'; unEl.style.color = ''; }
        icon.className = 'bi bi-eye';
        revealedEntries.delete(entryId);
        return;
    }

    try {
        icon.className = 'bi bi-hourglass-split';

        const resp = await fetch(`/vault/reveal/${entryId}`, {
            headers: typeof CSRF_HEADER !== 'undefined'
                ? { [CSRF_HEADER]: CSRF_TOKEN } : {}
        });

        if (!resp.ok) throw new Error('Access denied');

        const data = await resp.json();

        if (pwEl) {
            pwEl.textContent = data.password;
            pwEl.style.color = '#e6edf3';
            pwEl.style.fontFamily = 'Space Mono, monospace';
        }
        if (unEl) {
            unEl.textContent = data.username;
            unEl.style.color = '#e6edf3';
        }

        icon.className = 'bi bi-eye-slash';
        revealedEntries.add(entryId);

        // Auto-hide after 30 seconds
        setTimeout(() => {
            if (revealedEntries.has(entryId)) {
                if (pwEl) { pwEl.textContent = '••••••••'; pwEl.style.color = ''; }
                if (unEl) { unEl.textContent = '••••••••'; unEl.style.color = ''; }
                icon.className = 'bi bi-eye';
                revealedEntries.delete(entryId);
            }
        }, 30000);

    } catch (e) {
        icon.className = 'bi bi-eye';
        showToast('Failed to reveal entry.', 'danger');
    }
}

// ── Copy Password ────────────────────────────────────────────────────────────
async function copySecret(entryId) {
    try {
        const resp = await fetch(`/vault/reveal/${entryId}`, {
            headers: typeof CSRF_HEADER !== 'undefined'
                ? { [CSRF_HEADER]: CSRF_TOKEN } : {}
        });
        const data = await resp.json();

        await navigator.clipboard.writeText(data.password);
        showToast('Password copied to clipboard!', 'success');

        // Auto-clear clipboard after 30s
        setTimeout(() => navigator.clipboard.writeText(''), 30000);
    } catch (e) {
        showToast('Failed to copy.', 'danger');
    }
}

// ── Filter Entries ────────────────────────────────────────────────────────────
function filterEntries() {
    const search   = document.getElementById('searchInput')?.value?.toLowerCase() || '';
    const category = document.getElementById('categoryFilter')?.value?.toLowerCase() || '';
    const status   = document.getElementById('statusFilter')?.value || '';

    const rows = document.querySelectorAll('#entriesBody tr[data-title]');
    let visible = 0;

    rows.forEach(row => {
        const title       = (row.dataset.title || '').toLowerCase();
        const rowCategory = (row.dataset.category || '').toLowerCase();
        const isBreached  = row.dataset.breached === 'true';

        const matchSearch   = !search   || title.includes(search);
        const matchCategory = !category || rowCategory === category;
        const matchStatus   = !status   ||
            (status === 'safe'    && !isBreached) ||
            (status === 'breached' && isBreached);

        if (matchSearch && matchCategory && matchStatus) {
            row.style.display = '';
            visible++;
        } else {
            row.style.display = 'none';
        }
    });
}

// ── Delete Modal ──────────────────────────────────────────────────────────────
function confirmDelete(entryId, entryTitle) {
    const modal = document.getElementById('deleteModal');
    const msg   = document.getElementById('deleteModalMsg');
    const form  = document.getElementById('deleteForm');

    if (msg)  msg.textContent = `Delete "${entryTitle}" from your vault? This cannot be undone.`;
    if (form) form.action = `/vault/delete/${entryId}`;

    // Add CSRF token to form
    const token = document.getElementById('deleteToken');
    if (token) token.name = document.querySelector('meta[name="_csrf_parameter"]')?.content || '_csrf';

    if (modal) modal.style.display = 'flex';

    // Close on overlay click
    modal.onclick = (e) => { if (e.target === modal) closeDeleteModal(); };
}

function closeDeleteModal() {
    const modal = document.getElementById('deleteModal');
    if (modal) modal.style.display = 'none';
}

// ── Toast Notification ────────────────────────────────────────────────────────
function showToast(message, type = 'success') {
    const existing = document.getElementById('cv-toast');
    if (existing) existing.remove();

    const colors = {
        success: { bg: 'rgba(63,185,80,0.12)',  border: 'rgba(63,185,80,0.4)',  color: '#3fb950' },
        danger:  { bg: 'rgba(255,71,87,0.12)',  border: 'rgba(255,71,87,0.4)',  color: '#ff4757' },
        warning: { bg: 'rgba(255,214,10,0.08)', border: 'rgba(255,214,10,0.3)', color: '#ffd60a' }
    };

    const c = colors[type] || colors.success;
    const icons = { success: 'bi-check-circle-fill', danger: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill' };

    const toast = document.createElement('div');
    toast.id = 'cv-toast';
    toast.innerHTML = `<i class="bi ${icons[type] || 'bi-info-circle'} me-2"></i>${message}`;

    Object.assign(toast.style, {
        position:   'fixed',
        bottom:     '24px',
        right:      '24px',
        zIndex:     '9999',
        background: c.bg,
        border:     `1px solid ${c.border}`,
        color:      c.color,
        padding:    '12px 20px',
        borderRadius: '8px',
        fontFamily: 'Space Mono, monospace',
        fontSize:   '0.82rem',
        display:    'flex',
        alignItems: 'center',
        boxShadow:  '0 10px 30px rgba(0,0,0,0.4)',
        animation:  'toastIn 0.3s ease'
    });

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Inject toast animation
const toastStyle = document.createElement('style');
toastStyle.textContent = `
@keyframes toastIn {
    from { transform: translateX(20px); opacity: 0; }
    to   { transform: translateX(0);    opacity: 1; }
}`;
document.head.appendChild(toastStyle);

// ── Keyboard shortcut: Escape closes modal ────────────────────────────────────
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeDeleteModal();
});

// ── Auto-dismiss alerts ───────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.alert-dismissible').forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.4s';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 400);
        }, 5000);
    });
});
